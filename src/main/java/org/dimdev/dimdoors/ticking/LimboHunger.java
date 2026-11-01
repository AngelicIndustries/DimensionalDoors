package org.dimdev.dimdoors.ticking;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.FoodStats;

import org.dimdev.dimdoors.config.DDProperties;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Players don't get hungry in Limbo unless Hunger in Limbo is on. Their exhaustion is cleared before each of their
 * ticks, so it never adds up to the point where food or saturation drop; health still regenerates.
 */
public class LimboHunger {

    private static final String EXHAUSTION = "foodExhaustionLevel";

    private final DDProperties properties;

    public LimboHunger(DDProperties properties) {
        this.properties = properties;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || properties.HungerInLimbo
            || event.player.worldObj.isRemote
            || event.player.worldObj.provider.dimensionId != properties.LimboDimensionID) {
            return;
        }
        // FoodStats keeps its exhaustion private; its own NBT round trip is the public way to clear it
        FoodStats food = event.player.getFoodStats();
        NBTTagCompound tag = new NBTTagCompound();
        food.writeNBT(tag);
        if (tag.getFloat(EXHAUSTION) == 0) {
            return;
        }
        tag.setFloat(EXHAUSTION, 0);
        food.readNBT(tag);
    }
}
