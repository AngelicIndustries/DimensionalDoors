package org.dimdev.dimdoors.client;

import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;

import org.dimdev.dimdoors.world.LimboProvider;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Distant Horizons draws its far terrain without the game's fog, so in Limbo it shows through the thick black fog and
 * its stand-ins visibly turn into chunks. While the player is in Limbo with that fog, its rendering is switched off
 * through its API (an override it doesn't save) and back on when they leave. Reflective, so Distant Horizons stays
 * optional.
 */
@SideOnly(Side.CLIENT)
public final class DistantHorizonsInLimbo {

    private static final String API = "com.seibel.distanthorizons.api.";

    private boolean off;
    private boolean failed;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || failed) return;
        Minecraft mc = Minecraft.getMinecraft();
        boolean wanted = mc.theWorld != null && mc.theWorld.provider instanceof LimboProvider && LimboFog.density() > 0;
        if (wanted == off) return;
        try {
            Object configs = Class.forName(API + "DhApi$Delayed")
                .getField("configs")
                .get(null);
            // Not set up yet; try again next tick
            if (configs == null) return;
            Object graphics = Class.forName(API + "interfaces.config.IDhApiConfig")
                .getMethod("graphics")
                .invoke(configs);
            Object rendering = Class.forName(API + "interfaces.config.client.IDhApiGraphicsConfig")
                .getMethod("renderingEnabled")
                .invoke(graphics);
            Class<?> value = Class.forName(API + "interfaces.config.IDhApiConfigValue");
            if (wanted) {
                Method set = value.getMethod("setValue", Object.class);
                set.invoke(rendering, Boolean.FALSE);
            } else {
                value.getMethod("clearValue")
                    .invoke(rendering);
            }
            off = wanted;
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            FMLLog.warning("[dimdoors] Couldn't switch Distant Horizons off in Limbo: %s", e);
        }
    }
}
