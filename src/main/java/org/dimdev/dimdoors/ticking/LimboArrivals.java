package org.dimdev.dimdoors.ticking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import org.dimdev.dimdoors.core.DDTeleporter;
import org.dimdev.dimdoors.util.Point4D;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Players on their way into Limbo after dying. A death can happen while a world ticks its entities (a mob's attack, a
 * creeper), and moving a player to another world takes them out of that world's entity list at once, which throws the
 * loop off: it then removes the wrong entity, or crashes. So the move waits for the end of the server tick.
 */
public class LimboArrivals {

    private final Map<EntityPlayerMP, Point4D> pending = new LinkedHashMap<EntityPlayerMP, Point4D>();

    public void schedule(EntityPlayerMP player, Point4D destination) {
        pending.put(player, destination);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending.isEmpty()) {
            return;
        }
        List<Map.Entry<EntityPlayerMP, Point4D>> arrivals = new ArrayList<Map.Entry<EntityPlayerMP, Point4D>>(
            pending.entrySet());
        pending.clear();
        List<?> online = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        for (Map.Entry<EntityPlayerMP, Point4D> arrival : arrivals) {
            if (online.contains(arrival.getKey())) {
                DDTeleporter.teleportEntity(arrival.getKey(), arrival.getValue(), false);
            }
        }
    }
}
