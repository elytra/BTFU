package btfubackup

import java.io.File
import java.nio.file.{StandardCopyOption, Path, Files}
import BTFU.cfg

import scala.sys.process.Process
import scala.util.Try

trait FileActions {
  def delete(f: File): Boolean
  def hardlinkCopy(from: Path, to: Path): Boolean
  def sync(from: Path, to: Path): Boolean
}

object FileActions {
  def subdirectoryOf(sub: Path, parent: Path) = canonicalize(sub).startsWith(canonicalize(parent))
  def traverse(f: File, path: String): File = new File(s"$f/$path")
  def canonicalize(p: Path) = p.toAbsolutePath.normalize()
}

object ExternalCommandFileActions extends FileActions {
  override def delete(f: File) =
    Process(Seq(cfg.rm, "-r", f.getAbsolutePath)).run().exitValue() == 0

  override def hardlinkCopy(from: Path, to: Path) =
    Process(Seq(cfg.cp, "-al", from.toString, to.toString)).run().exitValue() == 0

  override def sync(from: Path, to: Path) =
    Process(Seq(cfg.rsync, "-ra", "--delete", from.toString, to.toString)).run().exitValue() == 0
}

object JvmNativeFileActions extends FileActions {
  override def delete(f: File) = Try(safeDelete(f.toPath, f)).isSuccess

  def safeDelete(prefix: Path, f: File): Unit = {
    if (! f.toPath.startsWith(prefix)) return

    if (f.isDirectory())
      f.listFiles().foreach(safeDelete(prefix, _))

    f.delete()
  }

  def definitelyMakeDir(d: File): Unit = {
    if (d.exists() && !d.isDirectory) delete(d)
    d.mkdirs()
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
    if (!from.startsWith(fromPrefix)) return
    val fromFile = from.toFile
    val to = toPrefix.resolve(fromPrefix.relativize(from))

    if (fromFile.isDirectory) definitelyMakeDir(to.toFile)

    main(fromPrefix, toPrefix, from, to)

    if (fromFile.isDirectory)
      fromFile.listFiles().foreach { f =>
        safeDualTraverse(main)(fromPrefix, toPrefix, f.toPath)
      }
  }

  override def hardlinkCopy(from: Path, to: Path) = Try {
    safeDualTraverse({ case (fromPrefix: Path, toPrefix: Path, from: Path, to: Path) =>
      if (! from.toFile.isDirectory) {
        Files.createLink(to, from)
      }
    })(from, to, from)
  }.isSuccess


  override def sync(from: Path, to: Path) = Try {
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
            copyWithAttributes(to, from)
          } else {
            if (toFile.lastModified() < fromFile.lastModified())
              copyWithAttributes(to, from)
          }
        } else {
          copyWithAttributes(to, from)
        }
      }
    })(from, to, from)
  }.isSuccess

  def copyWithAttributes(to: Path, from: Path) = Files.copy(to, from, StandardCopyOption.COPY_ATTRIBUTES)
}