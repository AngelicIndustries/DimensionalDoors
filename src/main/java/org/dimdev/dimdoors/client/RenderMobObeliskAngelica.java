package org.dimdev.dimdoors.client;

import com.gtnewhorizons.angelica.api.EyePassRenderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Monolith renderer marked as one with an eye pass, so Angelica draws the eye's glow with a shader pack's glowing
 * eyes program, as it does vanilla's spider eyes. Without it, packs such as Complementary light the glow like the
 * Monolith's skin: dull and dark. Only made when Angelica has the API (RenderMobObelisk.create).
 */
@SideOnly(Side.CLIENT)
public class RenderMobObeliskAngelica extends RenderMobObelisk implements EyePassRenderer {

    private RenderMobObeliskAngelica(float shadowSize) {
        super(shadowSize);
    }

    /** Typed as the plain renderer, so callers don't load this class (or Angelica's API) until they call it. */
    public static RenderMobObelisk create(float shadowSize) {
        return new RenderMobObeliskAngelica(shadowSize);
    }
}
