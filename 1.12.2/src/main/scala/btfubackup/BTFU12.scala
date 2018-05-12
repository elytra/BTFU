package btfubackup

import btfubackup.BTFU.cfg
import net.minecraft.server.MinecraftServer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.{FMLPreInitializationEvent, FMLServerAboutToStartEvent, FMLServerStoppingEvent}
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent
import net.minecraftforge.fml.common.{FMLCommonHandler, Mod}
import org.apache.logging.log4j.Logger

object LogWrapper12 extends LogWrapper {
  override def err(s: String): Unit = BTFU12.logger.error(s)
  override def warn(s: String): Unit = BTFU12.logger.warn(s)
  override def warn(s: String, e: Throwable): Unit = BTFU12.logger.warn(s, e)
}

@Mod(modid = "btfu", version = "1", name = "BTFU", modLanguage = "scala", acceptableRemoteVersions="*") object BTFU12 extends BTFU(LogWrapper12) {
  var logger:Logger = _

  import net.minecraftforge.fml.common.SidedProxy

  @SidedProxy(clientSide = "btfubackup.ClientProxy", serverSide = "btfubackup.ServerProxy") var proxy: CommonProxy = null

  @EventHandler
  def init(e: FMLPreInitializationEvent): Unit = {
    logger = e.getModLog

    cfg = BTFUConfig(ConfWrapper12(e.getSuggestedConfigurationFile))

    if (! handleStartupPathChecks()) {
      FMLCommonHandler.instance().exitJava(1, false)
    }

    val handler = new Object {
      @SubscribeEvent
      def onServerTick(event: ServerTickEvent): Unit = {
        if (event.phase == TickEvent.Phase.START) {
          WorldSavingControl12.mainThreadTick()
          BTFUPerformer.tick()
        }
      }
      @SubscribeEvent
      def join(e: PlayerLoggedInEvent) {
        BTFUPerformer.worldSavingControl.playersActive = true
      }
    }
    MinecraftForge.EVENT_BUS.register(handler)
  }

  @EventHandler
  def start(e: FMLServerAboutToStartEvent): Unit = {
    BTFU.serverLive = true
    BTFUPerformer.scheduleNextRun()
  }

  @EventHandler
  def stop(e: FMLServerStoppingEvent): Unit = {
    BTFU.serverLive = false
    BTFUPerformer.nextRun = None
  }

  override def getDediShlooper: Option[() => Option[String]] = proxy.getDedicatedServerInstance.map{ dedi =>
    { () =>
      while (dedi.pendingCommandList.isEmpty && dedi.isServerRunning) {
        Thread.sleep(25) // imagine a world where we use notify when we add to the threadsafe list.
      }
      if (!dedi.isServerRunning) return None // if the GUI window is closed
      Some(dedi.pendingCommandList.remove(0).command)
    }
  }
}