package btfubackup

import cpw.mods.fml.common.FMLCommonHandler
import net.minecraft.server.MinecraftServer
import net.minecraftforge.fml.common.FMLCommonHandler

object WorldSavingControl7 extends WorldSavingControl {
  override def realSaveTasks(t: Int) = {
    t match {
      case 1 => // save-off and save-all
        MinecraftServer.getServer.worldServers.foreach { worldserver =>
          if (worldserver != null) {
            worldserver.disableLevelSaving = false
            worldserver.saveAllChunks(true, null)
            worldserver.saveChunkData()
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

  override def getActivePlayerCount: Int = FMLCommonHandler.instance().getMinecraftServerInstance.getCurrentPlayerCount
}
