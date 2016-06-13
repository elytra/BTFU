package btfubackup

import net.minecraftforge.fml.common.FMLCommonHandler

object WorldSavingControl {
  def saveOffAndFlush() {
    FMLCommonHandler.instance.getMinecraftServerInstance.worldServers.foreach { worldserver =>
      if (worldserver != null) {
        worldserver.disableLevelSaving = false
        worldserver.saveAllChunks(true, null)
//        worldserver.saveChunkData()  TODO enable after https://github.com/MinecraftForge/FML/issues/679
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
