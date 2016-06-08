package btfubackup

import java.io.File
import net.minecraftforge.common.config.Configuration

case class BTFUConfig private (backupDir: File, meow: Int)

object BTFUConfig {
  def apply(f: File) = BTFUConfig(new Configuration(f))
  def apply(c: Configuration): BTFUConfig = {
    val conf = BTFUConfig(
      new File(c.get("backup directory", "BTFU", "").getString),
      2
    )
    if (c.hasChanged) c.save
    conf
  }
}
