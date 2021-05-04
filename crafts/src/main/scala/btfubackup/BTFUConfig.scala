package btfubackup

import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat


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
                               systemless: Boolean, excludes: Array[String], maxAgeSec: Long, debug: Boolean,
                               dateFormat: SimpleDateFormat, notDateFormat: SimpleDateFormat,
                               inactiveServerBackups: Boolean, c: ConfWrapper) {
  val mcDir: Path = FileActions.canonicalize(new File(".").toPath)

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
    val dateFormats = {
      val _dateFormats = Seq(".", ":").map{colon => new SimpleDateFormat(s"yyyy-MM-dd_HH${colon}mm")}
      if (c.getBoolean("BTFU", "windows-friendly datestamps", System.getProperty("os.name").startsWith("Windows"), "Use . instead of : in backup datestamps.  Turning this off on windows will cause a crash.  Turn it on if the backups need to be portable to windows."))
        _dateFormats
      else
        _dateFormats.reverse
    }
    val conf = BTFUConfig(
      c.getInt("BTFU", "number of backups to keep", 128),
      c.getBoolean("BTFU", "disable interactive prompts", false, "halt server instead of prompting at console on dedicated servers"),
      (BTFUNativeCommands.apply _).tupled(commands),
      systemless,
      c.getStringList("excluded paths", "BTFU", Array(),
        "For normal operation, see rsync manual for --exclude.  For systemless mode, see java.nio.file.PathMatcher.  " +
          "Patterns are for relative paths from the server root."),
      60L*60*24*c.getInt("BTFU", "Maximum backup age", -1, "Backups older than this many days will be deleted prior to logarithmic pruning, -1 to keep a complete history"),
      c.getBoolean("BTFU", "debug", false, "print additional information during backup tasks"),
      dateFormats(0), dateFormats(1),
      c.getBoolean("BTFU", "backup while empty", true, "keep taking backups even when nobody is playing on the server"),
      c
    )
    c.checkAndSave()
    conf
  }
}
