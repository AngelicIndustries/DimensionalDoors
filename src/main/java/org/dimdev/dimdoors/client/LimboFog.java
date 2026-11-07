package org.dimdev.dimdoors.client;

import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.world.LimboProvider;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Limbo's thick black fog, out of which a Monolith's eye opens close by. Linear, from a tenth of the sight distance to
 * black at it, so nothing shows past it: exponential fog never quite closes, and renderers like Angelica fog terrain
 * linearly anyway. Its colour comes from LimboProvider. Set by MixinEntityRendererLimboFog after the fog event, since
 * Dynamic Surroundings sets the fog range in that event at the lowest priority and would undo ours.
 */
@SideOnly(Side.CLIENT)
public class LimboFog {

    /** Where the fog starts, as a share of the distance where it's black. */
    private static final float START = 0.1F;
    /** The strength shader packs get until /limbofog sets one: a black sky, black at about 10 blocks. */
    static final double SHADER_DEFAULT = 1.6;
    /** The light shader packs get until /limbofog sets one. */
    static final double SHADER_LIGHT_DEFAULT = 0.3;

    /** The density /limbofog is trying out, or below 0 for the configured one. */
    static double tuning = -1;
    /** The shader pack strength /limbofog is trying out, or below 0 for the configured one. */
    static double shaderTuning = -1;
    /** The shader pack light /limbofog is trying out, or below 0 for the configured one. */
    static double shaderLightTuning = -1;

    public static double density() {
        return tuning >= 0 ? tuning : DimDoors.properties.LimboFogDensity;
    }

    /** How far one can see through the fog: as far as exponential fog of this density is about gone. */
    public static float sight(double density) {
        return (float) (3 / density);
    }

    /**
     * Limbo's fog under the shader pack in use, 0 to 2: up to 1 the pack's darkness effect, which blacks out its sky at
     * 1, past it blindness on top for thicker fog. How far that lets one see is the pack's call.
     */
    public static double shaderStrength() {
        if (shaderTuning >= 0) return shaderTuning;
        Double strength = forPack(DimDoors.properties.LimboShaderFog);
        return strength != null ? strength : SHADER_DEFAULT;
    }

    /**
     * Limbo's even light under the shader pack in use, 0 to 1, given to it as night vision: shader packs light the
     * world themselves and ignore the lightmap Limbo Ambient Light lifts.
     */
    public static double shaderLight() {
        if (shaderLightTuning >= 0) return shaderLightTuning;
        Double light = forPack(DimDoors.properties.LimboShaderLight);
        return light != null ? light : SHADER_LIGHT_DEFAULT;
    }

    /** The night vision a shader pack gets, 0 to 1. */
    public static float shaderNightVision() {
        return inLimbo() ? (float) Math.max(0, Math.min(1, shaderLight())) : 0;
    }

    /** The darkness a shader pack gets, 0 to 1 (MixinCommonUniformsLimboDarkness). */
    public static float shaderDarkness() {
        return inLimbo() ? (float) Math.min(1, shaderStrength()) : 0;
    }

    /** The blindness a shader pack gets on top, 0 to 1. */
    public static float shaderBlindness() {
        return inLimbo() ? (float) Math.max(0, Math.min(1, shaderStrength() - 1)) : 0;
    }

    /**
     * The value for the shader pack in use from a pack name=value list, or null: a name the pack's starts with, or for
     * a name in parts joined by "+", starts with the first part and contains the others
     * (ComplementaryUnbound+EuphoriaPatches), as AAC's per-dimension shader options are found. The longest name wins.
     */
    private static Double forPack(Map<String, Double> values) {
        String pack = ShaderPacks.name()
            .toLowerCase(Locale.ROOT);
        Double best = null;
        int bestLength = -1;
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            String key = entry.getKey()
                .toLowerCase(Locale.ROOT);
            if (key.length() <= bestLength) continue;
            String[] parts = key.split("\\+");
            boolean matches = pack.startsWith(parts[0]);
            for (int i = 1; matches && i < parts.length; i++) matches = pack.contains(parts[i]);
            if (matches) {
                best = entry.getValue();
                bestLength = key.length();
            }
        }
        return best;
    }

    private static boolean inLimbo() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.theWorld != null && mc.theWorld.provider instanceof LimboProvider;
    }

    /** Thickens the plain fog vanilla just set up, when the viewer is in Limbo. */
    public static void apply(Entity viewer, float farPlaneDistance) {
        double density = density();
        if (density <= 0 || viewer == null || !(viewer.worldObj.provider instanceof LimboProvider)) return;
        float end = Math.min(sight(density), farPlaneDistance);
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
        GL11.glFogf(GL11.GL_FOG_START, end * START);
        GL11.glFogf(GL11.GL_FOG_END, end);
    }
}
