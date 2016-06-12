package btfubackup

import java.io.File

import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.event.{FMLPreInitializationEvent, FMLServerAboutToStartEvent, FMLServerStoppingEvent}
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import cpw.mods.fml.common.{FMLCommonHandler, Mod}
import net.minecraft.crash.CrashReport
import net.minecraft.util.ReportedException
import net.minecraftforge.common.MinecraftForge
import org.apache.logging.log4j.LogManager

@Mod(modid = "BTFU", version = "1", name = "BTFU", modLanguage = "scala") object BTFU {
  var cfg:BTFUConfig = null
  var serverLive = false
  val logger = LogManager.getLogger("BTFU")

  @EventHandler
  def init(e: FMLPreInitializationEvent) = {
    cfg = BTFUConfig(e.getSuggestedConfigurationFile)
    if (! cfg.backupDir.exists())
      throw new ReportedException(new CrashReport(
        "Backups directory does not exist.  Configure it in BTFU.cfg", new RuntimeException))
    if (new File(".").getCanonicalPath.startsWith(cfg.backupDir.getCanonicalPath))
      throw new ReportedException(new CrashReport(
        "To run this backed up minecraft server, you must copy it outside the backups directory.", new RuntimeException))

    val handler = new Object {
      @SubscribeEvent
      def onServerTick(event: ServerTickEvent): Unit = {
        if (event.phase == TickEvent.Phase.START) {
          BTFUPerformer.tick()
        }
      }
    }
    MinecraftForge.EVENT_BUS.register(handler)
    FMLCommonHandler.instance().bus().register(handler)

    e.getModLog
  }

  @EventHandler
  def start(e: FMLServerAboutToStartEvent): Unit = {
    serverLive = true
    BTFUPerformer.scheduleNextRun
  }

  @EventHandler
  def stop(e: FMLServerStoppingEvent): Unit = {
    serverLive = false
    BTFUPerformer.nextRun = None
  }
}
