package btfubackup

import net.minecraft.server.MinecraftServer

object WorldSavingControl {
  def saveOffAndFlush() {
      MinecraftServer.getServer.worldServers.foreach { worldserver =>
      if (worldserver != null) {
        worldserver.disableLevelSaving = true
        worldserver.saveAllChunks(true, null)
        worldserver.saveChunkData()
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
