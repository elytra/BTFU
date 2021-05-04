package btfubackup

import java.io.{File, PrintWriter, StringWriter}
import java.text.SimpleDateFormat
import java.util.Date

import btfubackup.BTFU.{cfg, log}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

abstract class WorldSavingControl {
  BTFUPerformer.worldSavingControl = this
  def realSaveTasks(task: Int)

  private val lock = new Object()
  private var task:Int = 0

  /**
    * blocks while waiting for the save-off and flush to be performed on the main thread
    */
  def saveOffAndFlush() = waitPerformTask(1)

  /**
    * blocks while waiting for the save-on to be performed on the main thread
    */
  def restoreSaving() = waitPerformTask(2)

  protected def waitPerformTask(t: Int) {
    lock.synchronized{
      if (task != 0) throw new RuntimeException(s"Simultaneously scheduled WorldSavingControl tasks, $t and $task, are there multiple BTFU threads??")
      task = t
      lock.wait()
    }
  }

  /**
    * perform any scheduled tasks on the main thread
    */
  def mainThreadTick(): Unit = {
    lock.synchronized{
      if (task != 0) {
        realSaveTasks(task)
        task = 0
        lock.notify()
      }
    }
  }

  var playersActive = true
  def getActivePlayerCount: Int
}

object BTFUPerformer {
  var worldSavingControl: WorldSavingControl = _
  var nextRun: Option[Long] = None
  var backupProcess: Option[BackupProcess] = None


  def scheduleNextRun(): Unit = { nextRun = Some(System.currentTimeMillis + 1000 * 60 * 5) }

  def tick(): Unit = {
    backupProcess.foreach{ p =>
      if (p.isCompleted) {
        backupProcess = None
        if (BTFU.serverLive) scheduleNextRun()
      }
    }

    nextRun.foreach{ mils =>
      if (System.currentTimeMillis() >= mils && backupProcess.isEmpty) {
        backupProcess = Some(new BackupProcess())
        log.debug("Starting scheduled backup")
      }
    }
  }
}

class BackupProcess {
  private val fileActions = if (cfg.systemless) JvmNativeFileActions else ExternalCommandFileActions
  private val modelDir = cfg.backupDir.resolve("model")
  private val tmpDir = cfg.backupDir.resolve("tmp")

  private def datestampedBackups: List[(String, Long)] = FileActions.backupFilesFor(cfg.dateFormat).
      sortBy(_._2).reverse // sort by time since epoch descending

  private def deleteTmp() = deleteBackup("tmp") // clean incomplete backup copies
  private def deleteBackup(name: String) = fileActions.delete(new File(s"${cfg.backupDir}/$name"))

  val futureTask = Future{task()}
  private def task(): Unit = {
    try {
      /**
        * Phase 0: check playersActive
        */
      if(!cfg.inactiveServerBackups) {
        if(!BTFUPerformer.worldSavingControl.playersActive) return
        BTFUPerformer.worldSavingControl.playersActive = BTFUPerformer.worldSavingControl.getActivePlayerCount > 0
      }

      /**
        * Phase 1: trim backups
        */
      {
        deleteTmp()

        var backups = datestampedBackups
        if (backups.size >= 3 && cfg.maxAgeSec > 0) {
          val newestTime = backups.head._2
          backups.dropWhile { case (_, time) => newestTime - time < 1000 * cfg.maxAgeSec }.drop(1)
            .foreach { case (name, _) =>
              log.debug(s"Trimming old backup $name")
              deleteBackup(name)
            }
        }

        while ({backups = datestampedBackups; backups.length + 1 > cfg.maxBackups}) {
          val toRemove = backups.sliding(3).map {
            case List((_, d1), (s, _), (_, d0)) =>
              (s, 1000000*(backups.head._2 - d0)/(d1 - d0)) // fitness score for removal
          }.maxBy(_._2)._1
          log.debug(s"Trimming backup $toRemove")
          deleteBackup(s"$toRemove")
        }
      }

      /**
        * Phase 2: rsync
        */
      log.debug("Rsyncing...")
      BTFUPerformer.worldSavingControl.saveOffAndFlush()
      val backupDatestamp = System.currentTimeMillis() // used later
      val rsyncSuccess = fileActions.sync(cfg.mcDir, modelDir, BTFU.cfg.excludes)
      BTFUPerformer.worldSavingControl.restoreSaving()
      if (! rsyncSuccess) { // if we aborted here, we just have a partial rsync that can be corrected next time
        log.err("rsync failed")
        return
      }

      /**
        * Phase 3: hardlink copy
        */
      if (! fileActions.hardlinkCopy(modelDir, tmpDir)) {
        log.err("hardlink copy failed")
        deleteTmp()
        return
      }

      /**
        * Give the successful backup a date-name!
        */
      val datestr = BTFU.cfg.dateFormat.format(new Date(backupDatestamp))
      if (! tmpDir.toFile.renameTo(cfg.backupDir.resolve(datestr).toFile))
        log.err("rename failure??")
      else
        log.debug(s"backup success: $datestr")
    } catch {
      case e: Throwable => {
        val sw = new StringWriter
        e.printStackTrace(new PrintWriter(sw))
        log.err(sw.toString)
      }
    }
  }

  def isCompleted = futureTask.isCompleted
}
