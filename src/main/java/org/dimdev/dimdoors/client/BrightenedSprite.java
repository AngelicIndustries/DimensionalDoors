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
 * A texture drawn brighter than its file, made when the textures are stitched (so it follows resource packs). Unraveled
 * Fabric's texture is nearly black: Dimensional Doors drew Limbo with inverted, tripled lighting that lifted it, and
 * under Dark Limbo's real lighting it needs this to show a little of the light that falls on it.
 */
public class BrightenedSprite extends TextureAtlasSprite {

    private static final Logger LOG = LogManager.getLogger("DimDoors");

    private final ResourceLocation source;
    private final float factor;

    public BrightenedSprite(String name, ResourceLocation source, float factor) {
        super(name);
        this.source = source;
        this.factor = factor;
    }

    @Override
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return true;
    }

    /** Returns false once loaded, as the texture map expects of a custom loader. */
    @Override
    public boolean load(IResourceManager manager, ResourceLocation location) {
        BufferedImage image;
        try (InputStream in = manager.getResource(source)
            .getInputStream()) {
            image = ImageIO.read(in);
        } catch (IOException | RuntimeException e) {
            LOG.warn("Couldn't read {} for {}", source, getIconName(), e);
            image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }
        int size = image.getWidth();
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int color = image.getRGB(x, y);
                int r = Math.min(255, Math.round((color >> 16 & 255) * factor));
                int g = Math.min(255, Math.round((color >> 8 & 255) * factor));
                int b = Math.min(255, Math.round((color & 255) * factor));
                out.setRGB(x, y, color & 0xFF000000 | r << 16 | g << 8 | b);
            }
        }
        BufferedImage[] images = new BufferedImage[1 + Minecraft.getMinecraft().gameSettings.mipmapLevels];
        images[0] = out;
        loadSprite(images, null, Minecraft.getMinecraft().gameSettings.anisotropicFiltering > 1);
        return false;
    }
}
