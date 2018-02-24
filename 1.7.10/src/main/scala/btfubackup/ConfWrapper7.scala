package btfubackup
import java.io.File
import java.nio.file.Path

import net.minecraftforge.common.config.Configuration

object ConfWrapper7 {
  def apply(f: File): ConfWrapper = new ConfWrapper7(new Configuration(f))
}

class ConfWrapper7(c: Configuration) extends ConfWrapper {
  private val backupDirProp = c.get("BTFU", "backup directory", "")

  override def getBackupDir(): Path = FileActions.canonicalize(new File(backupDirProp.getString).toPath)

  def setBackupDir(path: Path): Unit = {
    backupDirProp.set(path.toString)
    checkAndSave()
  }

  override def getBoolean(option: String, section: String, defval: Boolean, desc: String): Boolean = c.getBoolean(option, section, defval, desc)
  override def getString(section: String, option: String, defval: String): String = c.get(section, option, defval).getString
  override def getInt(section: String, option: String, defval: Int, descr: String): Int = c.get(section, option, defval, descr).getInt(defval)
  override def getStringList(option: String, section: String, defval: Array[String], descr: String): Array[String] = c.getStringList(option, section, defval, descr)
  override def checkAndSave(): Unit = if (c.hasChanged) c.save()
}