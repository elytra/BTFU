package btfubackup

import java.io.File
import java.nio.file.Path

import net.minecraftforge.common.config.{Configuration, Property}

case class BTFUConfig (var backupDirProp: Property, maxBackups: Int, disablePrompts: Boolean, cmds: BTFUNativeCommands,
                               systemless: Boolean, excludes: Array[String], c: Configuration) {
  val mcDir = FileActions.canonicalize(new File(".").toPath)

  def backupDir: Path = {
    FileActions.canonicalize(new File(backupDirProp.getString).toPath)
  }

  def setBackupDir(path: Path): Unit = {
    backupDirProp.set(path.toString)
    if (c.hasChanged) c.save()
  }
}

case class BTFUNativeCommands(rsync: String, cp: String, rm: String)

object BTFUConfig {
  val RSYNC="rsync"
  val CP="cp"
  val RM="rm"

  def apply(f: File): BTFUConfig = BTFUConfig(new Configuration(f))
  def apply(c: Configuration): BTFUConfig = {
    val systemless = c.getBoolean("systemless", "system", true, "use jvm implementation for backup tasks (disable to use platform-native rsync/cp/rm)")
    val commands = if (systemless) (RSYNC, CP, RM) else ( // do not expose native tool path flags until systemless is disabled
      c.get("system", RSYNC, RSYNC).getString,
      c.get("system", CP, CP).getString,
      c.get("system", RM, RM).getString
    )
    val conf = BTFUConfig(
      c.get("BTFU", "backup directory", ""),
      c.get("BTFU", "number of backups to keep", 128).getInt(128),
      c.get("BTFU", "disable interactive prompts", 128, "halt server instead of prompting at console on dedicated servers").getBoolean(false),
      (BTFUNativeCommands.apply _).tupled(commands),
      systemless,
      c.getStringList("excluded paths", "BTFU", Array(),
        "For normal operation, see rsync manual for --exclude.  For systemless mode, see java.nio.file.PathMatcher.  " +
          "Patterns are for relative paths from the server root."),
      c
    )
    if (c.hasChanged) c.save()
    conf
  }
}
