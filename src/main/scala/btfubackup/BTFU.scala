package btfubackup

import java.io.File
import java.nio.file.Path

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.{FMLPreInitializationEvent, FMLServerAboutToStartEvent, FMLServerStoppingEvent}
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent
import net.minecraftforge.fml.common.{FMLCommonHandler, FMLLog, Mod}
import org.apache.logging.log4j.Logger

@Mod(modid = "btfu", version = "1", name = "BTFU", modLanguage = "scala", acceptableRemoteVersions="*") object BTFU {
  var cfg:BTFUConfig = null
  var logger:Logger = null
  var serverLive = false

  import net.minecraftforge.fml.common.SidedProxy

  @SidedProxy(clientSide = "btfubackup.ClientProxy", serverSide = "btfubackup.ServerProxy") var proxy: CommonProxy = null

  @EventHandler
  def init(e: FMLPreInitializationEvent): Unit = {
    logger = e.getModLog

    cfg = BTFUConfig(e.getSuggestedConfigurationFile)

    startupPathChecks(cfg.backupDir).foreach { error =>
      (if (cfg.disablePrompts) None else proxy.getDedicatedServerInstance) match {
        case Some(dedi) =>
          btfubanner()
          var pathCheck: Option[String] = Some(error)
          var enteredPath: Path = null
          do {
            logger.error(pathCheck.get)
            logger.error("Please enter a new path and press enter (or exit out and edit btfu.cfg)")

            val cmd = {
              while (dedi.pendingCommandList.isEmpty && dedi.isServerRunning) {
                Thread.sleep(25) // imagine a world where we use notify when we add to the threadsafe list.
              }
              if (!dedi.isServerRunning) return // if the GUI window is closed
              dedi.pendingCommandList.remove(0)
            }

            enteredPath = FileActions.canonicalize(new File(cmd.command).toPath)
            pathCheck = Some("buttholes")//startupPathChecks(path)
            pathCheck = startupPathChecks(enteredPath)
          } while (pathCheck.isDefined)

          logger.error(s"Awesome!  Your backups will go in $enteredPath.  I will shut up until something goes wrong!")
          cfg.setBackupDir(enteredPath)
        case None =>
          btfubanner()
          logger.error(s"/============================================================")
          logger.error(s"| $error")
          logger.error(s"| Please configure the backup path in btfu.cfg.")
          logger.error(s"\\============================================================")

          FMLCommonHandler.instance().exitJava(1, false)
      }
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
  }

  /**
    * @param path to check
    * @return Some(errormessage) if there is a problem, or None if the backup path is acceptable
    */
  def startupPathChecks(path: Path): Option[String] = {
    if (path.equals(cfg.mcDir))
      return Some("Backups directory is not set or matches your minecraft directory.")

    if (! path.toFile.exists())
      return Some(s"Backups directory ${'"'}$path${'"'} does not exist.")

    if (FileActions.subdirectoryOf(path, cfg.mcDir))
      return Some(s"Backups directory ${'"'}$path${'"'} is inside your minecraft directory ${'"'}${cfg.mcDir}${'"'}.\n" +
        s"This mod backups your entire minecraft directory, so that won't work.")

    if (FileActions.subdirectoryOf(cfg.mcDir, path))
      return Some(s"Backups directory ${'"'}$path${'"'} encompasses your minecraft server!\n" +
        s"(are you trying to run a backup without copying it, or back up to a directory your minecraft server is in?)")

    None
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

  def btfubanner(): Unit = {
    logger.error("               ,'\";-------------------;\"`.")
    logger.error("               ;[]; BBB  TTT FFF U  U ;[];")
    logger.error("               ;  ; B  B  T  F   U  U ;  ;")
    logger.error("               ;  ; B  B  T  F   U  U ;  ;")
    logger.error("               ;  ; BBB   T  FFF U  U ;  ;")
    logger.error("               ;  ; B  B  T  F   U  U ;  ;")
    logger.error("               ;  ; B  B  T  F   U  U ;  ;")
    logger.error("               ;  ; BBB   T  F    UU  ;  ;")
    logger.error("               ;  `.                 ,'  ;")
    logger.error("               ;    \"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"    ;")
    logger.error("               ;    ,-------------.---.  ;")
    logger.error("               ;    ;  ;\"\";       ;   ;  ;")
    logger.error("               ;    ;  ;  ;       ;   ;  ;")
    logger.error("               ;    ;  ;  ;       ;   ;  ;")
    logger.error("               ;//||;  ;  ;       ;   ;||;")
    logger.error("               ;\\\\||;  ;__;       ;   ;\\/;")
    logger.error("                `. _;          _  ;  _;  ;")
    logger.error("                  \" \"\"\"\"\"\"\"\"\"\"\" \"\"\"\"\" \"\"\"")
  }
}
