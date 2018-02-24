package btfubackup

import java.io.File
import java.nio.file.Path


trait ConfWrapper {
  def getBackupDir(): Path
  def setBackupDir(path: Path)
  def getBoolean(option: String, section: String, defval: Boolean, desc: String): Boolean
  def getString(section: String, option: String, defval: String): String
  def getInt(section: String, option: String, defval: Int, descr: String = null): Int
  def getStringList(option: String, section: String, defval: Array[String], descr: String): Array[String]
  def checkAndSave()
}

case class BTFUConfig(maxBackups: Int, disablePrompts: Boolean, cmds: BTFUNativeCommands,
                               systemless: Boolean, excludes: Array[String], maxAgeSec: Int, c: ConfWrapper) {
  val mcDir = FileActions.canonicalize(new File(".").toPath)

  def backupDir: Path = c.getBackupDir()
}

case class BTFUNativeCommands(rsync: String, cp: String, rm: String)

object BTFUConfig {
  val RSYNC="rsync"
  val CP="cp"
  val RM="rm"

  def apply(c: ConfWrapper): BTFUConfig = {
    val systemless = c.getBoolean("systemless", "system", true, "use jvm implementation for backup tasks (disable to use platform-native rsync/cp/rm)")
    val commands = if (systemless) (RSYNC, CP, RM) else ( // do not expose native tool path flags until systemless is disabled
      c.getString("system", RSYNC, RSYNC),
      c.getString("system", CP, CP),
      c.getString("system", RM, RM)
    )
    val conf = BTFUConfig(
      c.getInt("BTFU", "number of backups to keep", 128),
      c.getBoolean("BTFU", "disable interactive prompts", false, "halt server instead of prompting at console on dedicated servers"),
      (BTFUNativeCommands.apply _).tupled(commands),
      systemless,
      c.getStringList("excluded paths", "BTFU", Array(),
        "For normal operation, see rsync manual for --exclude.  For systemless mode, see java.nio.file.PathMatcher.  " +
          "Patterns are for relative paths from the server root."),
      60*60*24*c.getInt("BTFU", "Maximum backup age", -1, "Backups older than this many days will be deleted prior to logarithmic pruning, -1 to keep a complete history"),
      c
    )
    c.checkAndSave()
    conf
  }
}
