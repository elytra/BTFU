package btfubackup

import net.minecraft.server.MinecraftServer

object WorldSavingControl {
  def saveOffAndFlush() {
      MinecraftServer.getServer.worldServers.foreach { worldserver =>
      if (worldserver != null) {
        worldserver.disableLevelSaving = false
        worldserver.saveAllChunks(true, null)
        worldserver.saveChunkData()
        worldserver.disableLevelSaving = true
      }
    }
  }

  def restoreSaving() {
    MinecraftServer.getServer.worldServers.foreach { worldserver =>
      if (worldserver != null) {
        worldserver.disableLevelSaving = false
      }
    }
  }
}
