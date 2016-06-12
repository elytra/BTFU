package btfubackup

import java.io.File

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.{FMLServerStoppingEvent, FMLServerAboutToStartEvent, FMLPreInitializationEvent}
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent
import net.minecraftforge.fml.common.{FMLCommonHandler, FMLLog, Mod}
import org.apache.logging.log4j.Logger

@Mod(modid = "BTFU", version = "1", name = "BTFU", modLanguage = "scala") object BTFU {
  var cfg:BTFUConfig = null
  var logger:Logger = null
  var serverLive = false

  @EventHandler
  def init(e: FMLPreInitializationEvent) = {
    logger = e.getModLog

    cfg = BTFUConfig(e.getSuggestedConfigurationFile)
    if (! cfg.backupDir.exists()) {
      FMLLog.bigWarning("Backups directory does not exist.  Configure it in BTFU.cfg")
      FMLCommonHandler.instance().exitJava(1, false)
    }
    if (new File(".").getCanonicalPath.startsWith(cfg.backupDir.getCanonicalPath)) {
      FMLLog.bigWarning("To run this backed up minecraft server, you must copy it outside the backups directory.")
      FMLCommonHandler.instance().exitJava(1, true)
    }

    val handler = new Object {
      @SubscribeEvent
      def onServerTick(event: ServerTickEvent): Unit = {
        if (event.phase == TickEvent.Phase.START) {
          BTFUPerformer.tick()
        }
      }
    }
    MinecraftForge.EVENT_BUS.register(handler)

    e.getModLog
  }

  @EventHandler
  def start(e: FMLServerAboutToStartEvent): Unit = {
    serverLive = true
    BTFUPerformer.scheduleNextRun()
  }

  @EventHandler
  def stop(e: FMLServerStoppingEvent): Unit = {
    serverLive = false
    BTFUPerformer.nextRun = None
  }
}
