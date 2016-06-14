package btfubackup

import java.io.File
import net.minecraftforge.common.config.Configuration

case class BTFUConfig private (backupDir: File, maxBackups: Int, rsync: String, cp: String, rm: String)

object BTFUConfig {
  def apply(f: File): BTFUConfig = BTFUConfig(new Configuration(f))
  def apply(c: Configuration): BTFUConfig = {
    val conf = BTFUConfig(
      new File(c.get("BTFU", "backup directory", "").getString),
      c.get("BTFU", "number of backups to keep", 128).getInt(128),
      c.get("system", "rsync", "rsync").getString,
      c.get("system", "cp", "cp").getString,
      c.get("system", "rm", "rm").getString
    )
    if (c.hasChanged) c.save
    conf
  }
}
