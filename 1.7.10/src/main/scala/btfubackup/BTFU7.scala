package btfubackup

import btfubackup.BTFU.cfg
import cpw.mods.fml.common.{FMLCommonHandler, Mod, SidedProxy}
import cpw.mods.fml.common.Mod.EventHandler
import net.minecraftforge.common.MinecraftForge
import org.apache.logging.log4j.Logger
import cpw.mods.fml.common.event.{FMLPreInitializationEvent, FMLServerAboutToStartEvent, FMLServerStoppingEvent}
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import net.minecraft.command.ServerCommand

object LogWrapper7 extends LogWrapper {
  override def err(s: String): Unit = BTFU7.logger.error(s)
  override def warn(s: String): Unit = BTFU7.logger.warn(s)
  override def warn(s: String, e: Throwable): Unit = BTFU7.logger.warn(s, e)
}

@Mod(modid = "btfu", version = "1", name = "BTFU", modLanguage = "scala", acceptableRemoteVersions="*") object BTFU7 extends BTFU(LogWrapper7) {
  var logger:Logger = _


  @SidedProxy(clientSide = "btfubackup.ClientProxy", serverSide = "btfubackup.ServerProxy") var proxy: CommonProxy = null

  @EventHandler
  def init(e: FMLPreInitializationEvent): Unit = {
    logger = e.getModLog

    cfg = BTFUConfig(ConfWrapper7(e.getSuggestedConfigurationFile))

    if (! handleStartupPathChecks()) {
      FMLCommonHandler.instance().exitJava(1, false)
    }

    val handler = new Object {
      @SubscribeEvent
      def onServerTick(event: ServerTickEvent): Unit = {
        if (event.phase == TickEvent.Phase.START) {
          WorldSavingControl7.mainThreadTick()
          BTFUPerformer.tick()
        }
      }
      @SubscribeEvent
      def join(e: PlayerLoggedInEvent) {
        BTFUPerformer.worldSavingControl.playersActiveCountDown = cfg.numberInactiveBackups
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
      Some(dedi.pendingCommandList.remove(0).asInstanceOf[ServerCommand].command)
    }
  }
}