package org.dimdev.dimdoors.api.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import cpw.mods.fml.common.eventhandler.Cancelable;

/**
 * Fired on {@code MinecraftForge.EVENT_BUS}, on the server, when Dimensional Doors is about to turn a player's death
 * into a trip to Limbo. It comes from inside the {@link LivingDeathEvent}, before Dimensional Doors touches the
 * player's inventory or moves them.
 *
 * <p>
 * Cancel it to postpone Limbo, for example to offer a resurrection first. The death then happens as an ordinary one,
 * with the death screen, and the inventory drops or stays as vanilla rules say, whatever Limbo's own inventory
 * settings are. When the player respawns, Dimensional Doors sends them to Limbo, unless a {@link LimboRespawnEvent} is
 * cancelled.
 */
@Cancelable
public class LimboDeathEvent extends PlayerEvent {

    /** What killed the player. */
    public final DamageSource source;

    public LimboDeathEvent(EntityPlayer player, DamageSource source) {
        super(player);
        this.source = source;
    }
}
