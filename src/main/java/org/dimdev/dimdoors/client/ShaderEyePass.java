package org.dimdev.dimdoors.client;

import java.lang.reflect.Method;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Has a shader pack draw a glowing pass with its glowing eyes program, as Angelica does for spider eyes (Iris's
 * ENTITY_EYES condition). Without it, packs such as Complementary light the glow like the mob's skin: dim and tinted.
 * Reached by reflection, so it does nothing without Angelica or a shader pack.
 */
@SideOnly(Side.CLIENT)
final class ShaderEyePass {

    private static Method setup, teardown;
    private static Object eyes;
    private static boolean looked;

    private ShaderEyePass() {}

    /** Starts the eye pass, and says whether it did; end() must follow then. */
    static boolean begin() {
        if (!ShaderPacks.inUse()) return false;
        if (!looked) {
            looked = true;
            try {
                Class<?> programs = Class.forName("net.coderbot.iris.layer.GbufferPrograms");
                Class<?> condition = Class.forName("net.coderbot.iris.gbuffer_overrides.matching.SpecialCondition");
                eyes = condition.getField("ENTITY_EYES")
                    .get(null);
                teardown = programs.getMethod("teardownSpecialRenderCondition");
                setup = programs.getMethod("setupSpecialRenderCondition", condition);
            } catch (ReflectiveOperationException | LinkageError e) {
                setup = null;
            }
        }
        if (setup == null) return false;
        try {
            setup.invoke(null, eyes);
            return true;
        } catch (ReflectiveOperationException e) {
            setup = null;
            return false;
        }
    }

    static void end() {
        try {
            teardown.invoke(null);
        } catch (ReflectiveOperationException e) {
            setup = null;
        }
    }
}
