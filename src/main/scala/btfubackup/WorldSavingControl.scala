package btfubackup

import net.minecraft.server.MinecraftServer

object WorldSavingControl {
  def saveOffAndFlush() {
      MinecraftServer.getServer.worldServers.foreach { worldserver =>
      if (worldserver != null) {
        worldserver.levelSaving = true
        worldserver.saveAllChunks(true, null)
        worldserver.saveChunkData()
        worldserver.levelSaving = false
      }
    }
  }

  def restoreSaving() {
    MinecraftServer.getServer.worldServers.foreach { worldserver =>
      if (worldserver != null) {
        worldserver.levelSaving = true
      }
    }
  }
}
