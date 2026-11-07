package org.dimdev.dimdoors.api.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

import cpw.mods.fml.common.eventhandler.Cancelable;

/**
 * Fired on {@code MinecraftForge.EVENT_BUS}, on the server, when a player whose trip to Limbo was postponed through a
 * {@link LimboDeathEvent} respawns. {@link #entityPlayer} is the respawned player. Cancel it to keep them out of
 * Limbo, for example because the respawn is a resurrection.
 */
@Cancelable
public class LimboRespawnEvent extends PlayerEvent {

    public LimboRespawnEvent(EntityPlayer player) {
        super(player);
    }
}
