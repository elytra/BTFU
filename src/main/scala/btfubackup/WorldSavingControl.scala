package btfubackup

import net.minecraftforge.common.ForgeVersion
import net.minecraftforge.fml.common.FMLCommonHandler

object WorldSavingControl {
  def saveOffAndFlush() {
    FMLCommonHandler.instance.getMinecraftServerInstance.worldServers.foreach { worldserver =>
      if (worldserver != null) {
        worldserver.disableLevelSaving = false
        worldserver.saveAllChunks(true, null)
        if (ForgeVersion.buildVersion >= 1961) //  broken in earlier versions
          worldserver.saveChunkData()          // see https://github.com/MinecraftForge/FML/issues/679
        worldserver.disableLevelSaving = true
      }
    }
  }

  def restoreSaving() {
    FMLCommonHandler.instance.getMinecraftServerInstance.worldServers.foreach { worldserver =>
      if (worldserver != null) {
        worldserver.disableLevelSaving = false
      }
    }
  }
}
