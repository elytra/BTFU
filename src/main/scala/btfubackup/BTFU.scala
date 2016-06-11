package btfubackup

import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.event.{FMLPreInitializationEvent, FMLServerAboutToStartEvent, FMLServerStoppingEvent}
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import cpw.mods.fml.common.{FMLCommonHandler, Mod}
import net.minecraftforge.common.MinecraftForge
import org.apache.logging.log4j.LogManager

@Mod(modid = "BTFU", version = "1", name = "BTFU", modLanguage = "scala") object BTFU {
  var cfg:BTFUConfig = null
  var serverLive = false
  val logger = LogManager.getLogger("BTFU")

  @EventHandler
  def init(e: FMLPreInitializationEvent) = {
    cfg = BTFUConfig(e.getSuggestedConfigurationFile)

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
