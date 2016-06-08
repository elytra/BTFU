package btfubackup

import java.io.File
import java.text.SimpleDateFormat

import scala.actors.threadpool.Executors
import scala.concurrent.Future
import scala.sys.process.Process
import scala.util.{Success, Try}

object BTFUPerformer {
  var nextRun: Option[Int] = None
  val mcDir = new File(".")
  val dateMatcher = "d34".r
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd.HH:mm")

  val protectedBackups = 12 // most recent n backups are safe from deletion

  def scheduleNextRun: Unit = scheduleNextRun(1000 * 60 * 5)

  def scheduleNextRun(delay: Long): Unit = {
    val now = System.currentTimeMillis()

  }

  val pool = Executors.newFixedThreadPool(1)

  private object lock {
//    var phase: BackupPhase = RestingPhase
    var backupProcess: Option[BackupProcess] = None
  }

  def tick(): Unit = {

  }
}

class BackupProcess {
  object lock {
    var aborted: Boolean = false
    var process: Option[Process] = None
  }

  val futureTask = Future{task}

  private def task(): Unit = {
    /**
      * Phase 1: trim backups
      */
    val backups = BTFU.cfg.backupDir.list.toList.
      map { s => (s, Try{ BTFUPerformer.dateFormat.parse(s) }) }.
      collect { case (s, Success(d)) => (s, d) }

    while (backups.length + 1 > BTFU.cfg.maxBackups) {
      val backupsSorted = backups.sortBy(_._2).reverse
      val newest = backupsSorted.head._2.getTime

      val toRemove = backupsSorted.drop(BTFUPerformer.protectedBackups - 1).
        sliding(3).map {
          case List((_, d1), (s, _), (_, d0)) =>
            val d1t = d1.getTime
            (s, 1000000*(newest - d1t)/(d1t - d0.getTime)) // fitness score for removal
        }.maxBy(_._2)._1

      if (aborted) return // directory deletion can be a long operation
      new File(BTFU.cfg.backupDir + toRemove).delete()
      if (aborted) return
    }

    /**
      * Phase 2: save-off & save-all
      */
  }

  private def aborted: Boolean = {
    lock.synchronized { lock.aborted }
  }

  def isCompleted = futureTask.isCompleted

  def abort(): Unit = {
    lock.synchronized {
      lock.aborted = true
      lock.process.foreach(_.destroy())
    }
  }
}
