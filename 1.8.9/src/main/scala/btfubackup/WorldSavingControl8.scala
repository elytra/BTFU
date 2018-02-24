package btfubackup

import net.minecraft.server.MinecraftServer

object WorldSavingControl8 extends WorldSavingControl {
  override def realSaveTasks(t: Int) = {
    t match {
      case 1 => // save-off and save-all
        MinecraftServer.getServer.worldServers.foreach { worldserver =>
          if (worldserver != null) {
            worldserver.disableLevelSaving = false
            worldserver.saveAllChunks(true, null)
            //        worldserver.saveChunkData()  TODO enable after https://github.com/MinecraftForge/MinecraftForge/issues/2985 ... lol
            worldserver.disableLevelSaving = true
          }
        }

      case 2 => // save-on
        MinecraftServer.getServer.worldServers.foreach { worldserver =>
          if (worldserver != null) {
            worldserver.disableLevelSaving = false
          }
        }

      case _ => throw new IllegalArgumentException(s"internal error in WorldSavingControl: invalid task: $t")
    }
  }
}
