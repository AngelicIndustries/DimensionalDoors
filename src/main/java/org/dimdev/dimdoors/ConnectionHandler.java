package org.dimdev.dimdoors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.network.ForgeMessage;

import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.core.NewDimData;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.network.DimDoorsNetwork;
import org.dimdev.dimdoors.network.packets.ClientJoinPacket;
import org.dimdev.dimdoors.pockets.SharedPockets;
import org.dimdev.dimdoors.util.Point4D;
import org.dimdev.dimdoors.world.LimboProvider;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;

public class ConnectionHandler {

    @SubscribeEvent
    public void serverConnectionFromClientEvent(FMLNetworkEvent.ServerConnectionFromClientEvent event) {
        if (FMLCommonHandler.instance()
            .getSide() == Side.SERVER) {
            NetHandlerPlayServer server = ((NetHandlerPlayServer) event.handler);
            FMLEmbeddedChannel channel = NetworkRegistry.INSTANCE.getChannel("FORGE", Side.SERVER);
            for (NewDimData data : PocketManager.getDimensions()) {
                try {
                    if (data.isPocketDimension() || data.id() == DimDoors.properties.LimboDimensionID) {
                        channel.writeOutbound(
                            new ForgeMessage.DimensionRegisterMessage(
                                data.id(),
                                DimensionManager.getProviderType(data.id())));
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @SubscribeEvent
    public void connectionClosed(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        PocketManager.tryUnload();
    }

    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Hax... please don't do this! >_<
        DimDoorsNetwork.sendToPlayer(new ClientJoinPacket(), event.player);
        rescueFromResetPocket(event.player);
    }

    /** A player who logged out in a pocket that has been reset since goes to Limbo, or the overworld's spawn. */
    private static void rescueFromResetPocket(EntityPlayer player) {
        int dimension = player.worldObj.provider.dimensionId;
        if (!(player instanceof EntityPlayerMP) || !SharedPockets.isShared(dimension)
            || SharedPockets
                .pocketAt(dimension, MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posZ))
                != null) {
            return;
        }
        DDProperties properties = DDProperties.instance();
        Point4D destination;
        if (properties.LimboEnabled) {
            destination = LimboProvider.getLimboSkySpawn(player, properties);
        } else {
            World overworld = MinecraftServer.getServer()
                .worldServerForDimension(0);
            ChunkCoordinates spawn = overworld.getSpawnPoint();
            destination = new Point4D(
                spawn.posX,
                overworld.getTopSolidOrLiquidBlock(spawn.posX, spawn.posZ) + 1,
                spawn.posZ,
                0);
        }
        DimDoors.limboArrivals.schedule((EntityPlayerMP) player, destination);
    }
}
