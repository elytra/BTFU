package btfubackup

import net.minecraftforge.common.ForgeVersion
import net.minecraftforge.fml.common.FMLCommonHandler

object WorldSavingControl {
  private val lock = new Object()
  private var task:Int = 0

  /**
    * perform any scheduled tasks on the main thread
    */
  def mainThreadTick(): Unit = {
    lock.synchronized{
      if (task != 0) {
        realSaveTasks(task)
        task = 0
        lock.notify()
      }
    }
  }

  private def waitPerformTask(t: Int) {
    lock.synchronized{
      if (task != 0) throw new RuntimeException(s"Simultaneously scheduled WorldSavingControl tasks, $t and $task, are there multiple BTFU threads??")
      task = t
      lock.wait()
    }
  }

  /**
    * blocks while waiting for the save-off and flush to be performed on the main thread
    */
  def saveOffAndFlush() = waitPerformTask(1)

  /**
    * blocks while waiting for the save-on to be performed on the main thread
    */
  def restoreSaving() = waitPerformTask(2)

  private def realSaveTasks(t: Int) = {
    t match {
      case 1 => // save-off and save-all
        FMLCommonHandler.instance.getMinecraftServerInstance.worldServers.foreach { worldserver =>
          if (worldserver != null) {
            worldserver.disableLevelSaving = false
            worldserver.saveAllChunks(true, null)
            if (ForgeVersion.buildVersion >= 1961) //  broken in earlier versions
              worldserver.saveChunkData() // see https://github.com/MinecraftForge/FML/issues/679
            worldserver.disableLevelSaving = true
          }
        }
      case 2 => // save-on
        FMLCommonHandler.instance.getMinecraftServerInstance.worldServers.foreach { worldserver =>
          if (worldserver != null) {
            worldserver.disableLevelSaving = false
          }
        }
      case _ => throw new IllegalArgumentException(s"internal error in WorldSavingControl: invalid task: $t")
    }
  }
}
