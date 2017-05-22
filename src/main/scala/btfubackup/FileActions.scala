package btfubackup

import java.io.File
import java.nio.file.{StandardCopyOption, Path, Files}
import BTFU.{cfg, logger}

import scala.sys.process.Process
import scala.util.Try

trait FileActions {
  def delete(f: File): Boolean
  def hardlinkCopy(from: Path, to: Path): Boolean
  def sync(from: Path, to: Path): Boolean
}

object FileActions {
  def subdirectoryOf(sub: Path, parent: Path): Boolean = subdirectoryOf(sub.toFile, parent)
  def subdirectoryOf(sub: File, parent: Path): Boolean = sub.getCanonicalFile.toPath.startsWith(canonicalize(parent))
  def canonicalize(p: Path) = p.toFile.getCanonicalFile.toPath
}

object ExternalCommandFileActions extends FileActions {
  override def delete(f: File) = if (f.exists())
    Process(Seq(cfg.rm, "-r", f.getAbsolutePath)).run().exitValue() == 0 else false

  override def hardlinkCopy(from: Path, to: Path) =
    Process(Seq(cfg.cp, "-al", from.toString, to.toString)).run().exitValue() == 0

  override def sync(from: Path, to: Path) =
    Process(Seq(cfg.rsync, "-ra", "--delete", from.toString+"/", to.toString)).run().exitValue() == 0
}

object JvmNativeFileActions extends FileActions {
  override def delete(f: File) = Try(safeDelete(f.toPath, f)).isSuccess

  def safeDelete(prefix: Path, f: File): Unit = {
    if (! FileActions.subdirectoryOf(f, prefix)) return

    if (f.isDirectory())
      f.listFiles().foreach(safeDelete(prefix, _))

    f.delete()
  }

  def deleteNonFolder(d: File): Unit = {
    if (d.exists() && (!d.isDirectory || Files.isSymbolicLink(d.toPath))) delete(d)
  }

  def definitelyMakeDir(d: File): Unit = {
    deleteNonFolder(d)
    d.mkdirs()
  }

  def definitelyMakeSymlink(p: Path, linkDest: Path): Unit = {
    deleteNonFolder(p.toFile)
    Files.createSymbolicLink(p, linkDest)
  }

  /**
    * Traverse subtree from, based within fromPrefix, and mirror directory structure to toPrefix.
    * At each step:
    *  if from is a directory, creates a directory at to
    *  calls main(fromPrefix, toPrefix, from, to)
    *  if from is a directory, recurses
    *
    * @param main
    * @param fromPrefix
    * @param toPrefix
    * @param from
    */
  def safeDualTraverse(main: (Path, Path, Path, Path) => Unit)(fromPrefix: Path, toPrefix: Path, from: Path): Unit = {
    if (!FileActions.subdirectoryOf(from, fromPrefix)) return
    val fromFile = from.toFile
    val to = toPrefix.resolve(fromPrefix.relativize(from))

    if (fromFile.isDirectory) definitelyMakeDir(to.toFile)

    main(fromPrefix, toPrefix, from, to)

    if (fromFile.isDirectory)
      fromFile.listFiles().foreach { f =>
        val path = f.toPath
        if (Files.isSymbolicLink(path))
          definitelyMakeSymlink(toPrefix.resolve(fromPrefix.relativize(path)), Files.readSymbolicLink(path))
        else
          safeDualTraverse(main)(fromPrefix, toPrefix, path)
      }
  }

  override def hardlinkCopy(from: Path, to: Path) = Try {
    safeDualTraverse({ case (fromPrefix: Path, toPrefix: Path, from: Path, to: Path) =>
      if (! from.toFile.isDirectory) {
        Files.createLink(to, from)
      }
    })(from, to, from)
  }.isSuccess


  override def sync(from: Path, to: Path) = {
    val t = Try {
      safeDualTraverse({ case (fromPrefix: Path, toPrefix: Path, from: Path, to: Path) =>
        val fromFile = from.toFile; val toFile = to.toFile
        if (fromFile.isDirectory) {
          toFile.list()
            .filterNot(fromFile.list().toSet) // extraneous files in to but not from
            .map(to.resolve).map(_.toFile)
            .foreach(delete)
        } else {
          if (toFile.exists()) {
            if (toFile.isDirectory) {
              delete(toFile)
              copyWithAttributes(from, to)
            } else {
              if (toFile.lastModified() < fromFile.lastModified())
                copyWithAttributes(from, to)
            }
          } else {
            copyWithAttributes(from, to)
          }
        }
      })(from, to, from)
    }
    t.recover{case e => logger.warn("Exception in fake rsync", e)}
    t.isSuccess
  }

  def copyWithAttributes(from: Path, to: Path) = Files.copy(from, to, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
}