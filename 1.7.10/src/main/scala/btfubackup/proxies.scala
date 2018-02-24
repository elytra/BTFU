package btfubackup

import cpw.mods.fml.common.FMLCommonHandler
import net.minecraft.server.dedicated.DedicatedServer

trait CommonProxy {
  def getDedicatedServerInstance: Option[DedicatedServer]
}

class ClientProxy extends CommonProxy {
  override def getDedicatedServerInstance: Option[DedicatedServer] = None
}

class ServerProxy extends CommonProxy {
  override def getDedicatedServerInstance =
    Some(FMLCommonHandler.instance().getMinecraftServerInstance) collect {case dedi: DedicatedServer => dedi}
}
