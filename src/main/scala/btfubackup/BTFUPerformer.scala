package btfubackup

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import scala.concurrent.Future
import scala.sys.process.{Process, ProcessBuilder}
import scala.util.{Success, Try}

import scala.concurrent.ExecutionContext.Implicits.global

object BTFUPerformer {
  var nextRun: Option[Long] = None
  var backupProcess: Option[BackupProcess] = None

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm")

  val mcDir = new File(".")
  val modelDir = s"${BTFU.cfg.backupDir}/model"
  val tmpDir = s"${BTFU.cfg.backupDir}/tmp"

  val rsyncCmd = Process(Seq(BTFU.cfg.rsync, "-ra", BTFUPerformer.mcDir.getAbsolutePath, modelDir))
  val hardlinkCmd = Process(Seq(BTFU.cfg.cp, "-al", modelDir, tmpDir))

  def scheduleNextRun(): Unit = scheduleNextRun(1000 * 60 * 5)
  def scheduleNextRun(delay: Long): Unit = { nextRun = Some(System.currentTimeMillis + delay) }

  def tick(): Unit = {
    backupProcess.foreach{ p =>
      if (p.isCompleted) {
        backupProcess = None
        if (BTFU.serverLive && !p.aborted) scheduleNextRun()
      }
    }

    nextRun.foreach{ mils =>
      if (System.currentTimeMillis() >= mils && backupProcess.isEmpty) {
        backupProcess = Some(new BackupProcess())
        BTFU.logger.debug("Starting scheduled backup")
      }
    }
  }
}

class BackupProcess {
  private object lock {
    var aborted: Boolean = false
    var externalProcess: Option[Process] = None
  }

  private def datestampedBackups: List[(String, Long)] = BTFU.cfg.backupDir.list.toList.
      map { s => (s, Try{ BTFUPerformer.dateFormat.parse(s).getTime }) }.
      collect { case (s, Success(d)) => (s, d) }.
      sortBy(_._2).reverse // sort by time since epoch descending

  private def deleteFileExternal(f: File) = if (f.exists) Process(Seq(BTFU.cfg.rm, "-r", f.getAbsolutePath)).run().exitValue()
  private def deleteTmp() = deleteFileExternal(new File(s"${BTFU.cfg.backupDir}/tmp")) // clean incomplete backup copies

  val futureTask = Future{task()}
  private def task(): Unit = {
    /**
      * Phase 1: trim backups
      */
    {
      deleteTmp()
      var backups = datestampedBackups
      while (backups.length + 1 > BTFU.cfg.maxBackups) {
        val toRemove = backups.sliding(3).map {
          case List((_, d1), (s, _), (_, d0)) =>
            (s, 1000000*(backups.head._2 - d0)/(d1 - d0)) // fitness score for removal
        }.maxBy(_._2)._1
        BTFU.logger.debug(s"Trimming backup $toRemove")
        deleteFileExternal(new File(s"${BTFU.cfg.backupDir}/$toRemove"))
        if (aborted) return
        backups = datestampedBackups
      }
    }

    /**
      * Phase 2: rsync
      */
    BTFU.logger.debug("Rsyncing...")
    WorldSavingControl.saveOffAndFlush()
    val backupDatestamp = System.currentTimeMillis() // used later
    val rsyncSuccess = runProcessAbortable(BTFUPerformer.rsyncCmd)
    WorldSavingControl.restoreSaving()
    if (! rsyncSuccess) { // if we aborted here, we just have a partial rsync that can be corrected next time
      BTFU.logger.warn("rsync failed")
      return
    }
    BTFU.logger.debug("rsync success")

    /**
      * Phase 3: hardlink copy
      */
    val cpSuccess = runProcessAbortable(BTFUPerformer.hardlinkCmd)
    if (! cpSuccess) {
      deleteTmp()
      BTFU.logger.warn("hardlink copy failed")
      return
    }

    /**
      * Give the successful backup a date-name!
      */
    val datestr = BTFUPerformer.dateFormat.format(new Date(backupDatestamp))
    new File(BTFUPerformer.tmpDir).renameTo(
      new File(s"${BTFU.cfg.backupDir}/$datestr")
    )
    BTFU.logger.debug(s"backup success: $datestr")
  }


  def aborted: Boolean = lock.synchronized { lock.aborted }

  /**
    * @param cmd Seq("command", "arg1", "arg2"...)
    * @return true if process is completed without abortion and has exit code 0
    */
  private def runProcessAbortable(cmd: ProcessBuilder): Boolean = {
    lock.synchronized {
      val p = if (lock.aborted) None else Some(cmd.run())
      lock.externalProcess = p
      p
    }.map { p =>
      val e = p.exitValue
      lock.synchronized{ lock.externalProcess = None }
      e
    }.contains(0)
  }

  def isCompleted = futureTask.isCompleted

  def abort(): Unit = {
    lock.synchronized {
      lock.aborted = true
      lock.externalProcess.foreach(_.destroy())
    }
  }
}
