package org.dimdev.dimdoors.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Fadebloom's textures, made from the blue orchid's when the block textures are stitched (so they follow
 * resource packs): its stem turned grey-purple and its petals pale grey, each keeping the original's shading. The
 * petals are a texture of their own, drawn brighter than the light around them.
 */
public class FadebloomSprite extends TextureAtlasSprite {

    public enum Part {
        STEM,
        PETALS,
        WHOLE
    }

    private static final Logger LOG = LogManager.getLogger("DimDoors");
    private static final ResourceLocation ORCHID = new ResourceLocation(
        "minecraft",
        "textures/blocks/flower_blue_orchid.png");
    private static final int[] STEM_COLOR = { 112, 98, 128 };
    private static final int[] PETAL_COLOR = { 214, 214, 224 };

    private final Part part;

    public FadebloomSprite(String name, Part part) {
        super(name);
        this.part = part;
    }

    @Override
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return true;
    }

    /** Returns false once loaded, as the texture map expects of a custom loader. */
    @Override
    public boolean load(IResourceManager manager, ResourceLocation location) {
        BufferedImage orchid;
        try (InputStream in = manager.getResource(ORCHID)
            .getInputStream()) {
            orchid = ImageIO.read(in);
        } catch (IOException | RuntimeException e) {
            LOG.warn("Couldn't read the blue orchid's texture for {}", getIconName(), e);
            orchid = null;
        }
        int size = orchid == null ? 16 : orchid.getWidth();
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        if (orchid != null) recolor(orchid, out, size);
        BufferedImage[] images = new BufferedImage[1 + Minecraft.getMinecraft().gameSettings.mipmapLevels];
        images[0] = out;
        loadSprite(images, null, Minecraft.getMinecraft().gameSettings.anisotropicFiltering > 1);
        return false;
    }

    private void recolor(BufferedImage orchid, BufferedImage out, int size) {
        double stemTotal = 0, petalTotal = 0;
        int stemCount = 0, petalCount = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int color = orchid.getRGB(x, y);
                if (color >>> 24 == 0) continue;
                if (isPetal(color)) {
                    petalTotal += luminance(color);
                    petalCount++;
                } else {
                    stemTotal += luminance(color);
                    stemCount++;
                }
            }
        }
        double stemMean = stemCount == 0 ? 1 : stemTotal / stemCount;
        double petalMean = petalCount == 0 ? 1 : petalTotal / petalCount;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int color = orchid.getRGB(x, y);
                if (color >>> 24 == 0) continue;
                boolean petal = isPetal(color);
                if (part == Part.STEM && petal || part == Part.PETALS && !petal) continue;
                double shade = luminance(color) / (petal ? petalMean : stemMean);
                out.setRGB(x, y, tint(color >>> 24, petal ? PETAL_COLOR : STEM_COLOR, shade));
            }
        }
    }

    /** The orchid's petals are blue, its stem and leaves green. */
    private static boolean isPetal(int color) {
        return (color & 255) > (color >> 8 & 255);
    }

    private static double luminance(int color) {
        return 0.299 * (color >> 16 & 255) + 0.587 * (color >> 8 & 255) + 0.114 * (color & 255);
    }

    private static int tint(int alpha, int[] base, double shade) {
        int r = Math.min(255, (int) Math.round(base[0] * shade));
        int g = Math.min(255, (int) Math.round(base[1] * shade));
        int b = Math.min(255, (int) Math.round(base[2] * shade));
        return alpha << 24 | r << 16 | g << 8 | b;
    }
}
