package btfubackup

import java.io.File
import java.nio.file.Path
import net.minecraftforge.common.config.Configuration

case class BTFUConfig private (backupDir: Path, maxBackups: Int, rsync: String, cp: String, rm: String,
                               systemless: Boolean, excludes: Array[String]) {
  val mcDir = FileActions.canonicalize(new File(".").toPath)
}

object BTFUConfig {
  def apply(f: File): BTFUConfig = BTFUConfig(new Configuration(f))
  def apply(c: Configuration): BTFUConfig = {
    val conf = BTFUConfig(
      FileActions.canonicalize(new File(c.get("BTFU", "backup directory", "").getString).toPath),
      c.get("BTFU", "number of backups to keep", 128).getInt(128),
      c.get("system", "rsync", "rsync").getString,
      c.get("system", "cp", "cp").getString,
      c.get("system", "rm", "rm").getString,
      c.getBoolean("systemless", "system", false, "ignores platform-native tools and uses simple jvm-based implementations"),
      c.getStringList("excluded paths", "BTFU", Array(),
        "For normal operation, see rsync manual for --exclude.  For systemless mode, see java.nio.file.PathMatcher.  " +
          "Patterns are for relative paths from the server root.")
    )
    if (c.hasChanged) c.save
    conf
  }
}
