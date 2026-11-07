package org.dimdev.dimdoors.client;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.util.ResourceLocation;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.mixins.AccessorShaderGroup;
import org.dimdev.dimdoors.ticking.MobMonolith;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The screen of a player a Monolith is about to take (the red particles): its edges pulse red and it breaks up in red
 * glitches, worse the further the Monolith's aggro has gone, up to Monolith Aggro Screen Effect. A post shader
 * (shaders/post/monolith_aggro.json) drawn right after the world (MixinEntityRendererMonolithAggro), so over any
 * shader pack too.
 */
@SideOnly(Side.CLIENT)
public class MonolithAggroEffect {

    private static final ResourceLocation SHADER = new ResourceLocation(
        DimDoors.modid,
        "shaders/post/monolith_aggro.json");
    /**
     * How much of the effect shows as the aggro starts; all of it shows as the Monolith is about to take the player.
     */
    private static final float START = 0.35F;
    private static final long STARTED = System.nanoTime();

    private static float intensity, previousIntensity;
    private static ShaderGroup group;
    private static int width, height;
    private static boolean failed;
    private static long lastFrame;
    private static double pulsePhase;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        previousIntensity = intensity;
        float target = 0;
        Minecraft mc = Minecraft.getMinecraft();
        double strength = DimDoors.properties.MonolithAggroScreenEffect;
        if (mc.theWorld != null && mc.thePlayer != null && strength > 0) {
            for (Object entity : mc.theWorld.loadedEntityList) {
                if (!(entity instanceof MobMonolith)) continue;
                MobMonolith monolith = (MobMonolith) entity;
                if (!monolith.isDangerous() || !monolith.isAggroed()
                    || monolith.getDistanceToEntity(mc.thePlayer) > DimDoors.properties.MonolithAggroRange) continue;
                target = Math.max(target, (float) (strength * (START + (1 - START) * monolith.getAggroDepth())));
            }
        }
        // Comes on quickly, wears off a little slower
        intensity = target > intensity ? Math.min(target, intensity + 0.08F) : Math.max(target, intensity - 0.04F);
    }

    /** Draws the effect over the world just drawn, if it shows at all. */
    public static void render(float partialTicks) {
        float now = previousIntensity + (intensity - previousIntensity) * partialTicks;
        if (now <= 0.001F || failed || !OpenGlHelper.shadersSupported) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (group == null) {
                group = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), SHADER);
                width = -1;
            }
            if (width != mc.displayWidth || height != mc.displayHeight) {
                width = mc.displayWidth;
                height = mc.displayHeight;
                group.createBindFramebuffers(width, height);
            }
        } catch (IOException | RuntimeException e) {
            failed = true;
            FMLLog.warning("[DimDoors] Couldn't load the Monolith aggro screen effect: %s", e);
            return;
        }

        long frame = System.nanoTime();
        double seconds = lastFrame == 0 ? 0 : Math.min(1, (frame - lastFrame) / 1e9);
        lastFrame = frame;
        // A heartbeat that quickens as it worsens
        pulsePhase = (pulsePhase + seconds * (0.8 + 1.6 * now) * 2 * Math.PI) % (2 * Math.PI);
        float pulse = (float) (0.5 + 0.5 * Math.sin(pulsePhase));
        float clock = (float) ((frame - STARTED) / 1e9 % 1000);
        for (Object shader : ((AccessorShaderGroup) group).getListShaders()) {
            ShaderManager manager = ((Shader) shader).getShaderManager();
            manager.func_147984_b("Intensity")
                .func_148090_a(now);
            manager.func_147984_b("Pulse")
                .func_148090_a(pulse);
            manager.func_147984_b("Clock")
                .func_148090_a(clock);
        }
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        group.loadShaderGroup(partialTicks);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        mc.getFramebuffer()
            .bindFramebuffer(true);
    }
}
