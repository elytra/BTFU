package btfubackup

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import scala.concurrent.Future
import scala.util.{Success, Try}

import BTFU.cfg

import scala.concurrent.ExecutionContext.Implicits.global

object BTFUPerformer {
  var nextRun: Option[Long] = None
  var backupProcess: Option[BackupProcess] = None

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm")

  def scheduleNextRun(): Unit = { nextRun = Some(System.currentTimeMillis + 1000 * 60 * 5) }

  def tick(): Unit = {
    WorldSavingControl.mainThreadTick()

    backupProcess.foreach{ p =>
      if (p.isCompleted) {
        backupProcess = None
        if (BTFU.serverLive) scheduleNextRun()
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
  val fileActions = if (cfg.systemless) JvmNativeFileActions else ExternalCommandFileActions
  val modelDir = cfg.backupDir.resolve("model")
  val tmpDir = cfg.backupDir.resolve("tmp")

  private def datestampedBackups: List[(String, Long)] = cfg.backupDir.toFile.list.toList.
      map { s => (s, Try{ BTFUPerformer.dateFormat.parse(s).getTime }) }.
      collect { case (s, Success(d)) => (s, d) }.
      sortBy(_._2).reverse // sort by time since epoch descending

  private def deleteTmp() = fileActions.delete(new File(s"${cfg.backupDir}/tmp")) // clean incomplete backup copies

  val futureTask = Future{task()}
  private def task(): Unit = {
    /**
      * Phase 1: trim backups
      */
    {
      deleteTmp()
      var backups = datestampedBackups
      while (backups.length + 1 > cfg.maxBackups) {
        val toRemove = backups.sliding(3).map {
          case List((_, d1), (s, _), (_, d0)) =>
            (s, 1000000*(backups.head._2 - d0)/(d1 - d0)) // fitness score for removal
        }.maxBy(_._2)._1
        BTFU.logger.debug(s"Trimming backup $toRemove")
        fileActions.delete(new File(s"${cfg.backupDir}/$toRemove"))
        backups = datestampedBackups
      }
    }

    /**
      * Phase 2: rsync
      */
    BTFU.logger.debug("Rsyncing...")
    WorldSavingControl.saveOffAndFlush()
    val backupDatestamp = System.currentTimeMillis() // used later
    val rsyncSuccess = fileActions.sync(cfg.mcDir, modelDir)
    WorldSavingControl.restoreSaving()
    if (! rsyncSuccess) { // if we aborted here, we just have a partial rsync that can be corrected next time
      BTFU.logger.warn("rsync failed")
      return
    }

    /**
      * Phase 3: hardlink copy
      */
    if (! fileActions.hardlinkCopy(modelDir, tmpDir)) {
      BTFU.logger.warn("hardlink copy failed")
      deleteTmp()
      return
    }

    /**
      * Give the successful backup a date-name!
      */
    val datestr = BTFUPerformer.dateFormat.format(new Date(backupDatestamp))
    tmpDir.toFile.renameTo(
      new File(s"${cfg.backupDir}/$datestr")
    )
    BTFU.logger.debug(s"backup success: $datestr")
  }

  def isCompleted = futureTask.isCompleted
}
