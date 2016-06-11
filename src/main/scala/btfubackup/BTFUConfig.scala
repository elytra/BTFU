package btfubackup

import java.io.File
import net.minecraftforge.common.config.Configuration

case class BTFUConfig private (backupDir: File, maxBackups: Int, rsync: String, cp: String)

object BTFUConfig {
  def apply(f: File): BTFUConfig = BTFUConfig(new Configuration(f))
  def apply(c: Configuration): BTFUConfig = {
    val conf = BTFUConfig(
      new File(c.get("backup directory", "BTFU", "").getString),
      c.get("number of backups to keep", "BTFU", 128).getInt(128),
      c.get("rsync", "system", "rsync").getString,
      c.get("cp", "system", "cp").getString
    )
    if (c.hasChanged) c.save
    conf
  }
}
