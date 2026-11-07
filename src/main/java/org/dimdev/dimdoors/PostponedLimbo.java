package org.dimdev.dimdoors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;

import org.dimdev.dimdoors.api.event.LimboDeathEvent;
import org.dimdev.dimdoors.api.event.LimboRespawnEvent;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.world.LimboProvider;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Players whose trip to Limbo another mod postponed through {@link LimboDeathEvent}: they die as usual and go to Limbo
 * when they respawn, unless a {@link LimboRespawnEvent} is cancelled. The mark lives in the player's persisted data,
 * so it survives the death and a logout from the death screen.
 */
public class PostponedLimbo {

    private static final String KEY = "dimdoorsPostponedLimbo";

    private final DDProperties properties;

    public PostponedLimbo(DDProperties properties) {
        this.properties = properties;
    }

    /** Lets other mods postpone Limbo for a dying player, and returns whether one did. */
    public static boolean postpone(EntityPlayer player, DamageSource source) {
        if (!MinecraftForge.EVENT_BUS.post(new LimboDeathEvent(player, source))) {
            return false;
        }
        NBTTagCompound data = player.getEntityData();
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        persisted.setBoolean(KEY, true);
        data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        return true;
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.player instanceof EntityPlayerMP player) || !take(player)) {
            return;
        }
        if (!MinecraftForge.EVENT_BUS.post(new LimboRespawnEvent(player))) {
            DimDoors.limboArrivals.schedule(player, LimboProvider.getLimboSkySpawn(player, properties));
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // A respawned player loses the mark before its first tick, so a living one still carrying it was saved by a
        // mod that cancelled the death after Limbo was postponed, and owes Limbo nothing.
        if (event.phase == TickEvent.Phase.START && !event.player.worldObj.isRemote
            && event.player.isEntityAlive()
            && event.player.ticksExisted % 20 == 0) {
            take(event.player);
        }
    }

    private static boolean take(EntityPlayer player) {
        NBTTagCompound persisted = player.getEntityData()
            .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!persisted.getBoolean(KEY)) {
            return false;
        }
        persisted.removeTag(KEY);
        return true;
    }
}
