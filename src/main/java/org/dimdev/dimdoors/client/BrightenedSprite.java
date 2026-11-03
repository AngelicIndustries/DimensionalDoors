package org.dimdev.dimdoors.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
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
    private final int variant;

    public BrightenedSprite(String name, ResourceLocation source, float factor) {
        this(name, source, factor, 0);
    }

    /** A turned, mirrored or time-shifted copy of the texture as well, see {@link SpriteFrames#variant}. */
    public BrightenedSprite(String name, ResourceLocation source, float factor, int variant) {
        super(name);
        this.source = source;
        this.factor = factor;
        this.variant = variant;
    }

    @Override
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return true;
    }

    /** Returns false once loaded, as the texture map expects of a custom loader. */
    @Override
    public boolean load(IResourceManager manager, ResourceLocation location) {
        BufferedImage image;
        AnimationMetadataSection animation = null;
        try {
            IResource resource = manager.getResource(source);
            try (InputStream in = resource.getInputStream()) {
                image = ImageIO.read(in);
            }
            animation = (AnimationMetadataSection) resource.getMetadata("animation");
        } catch (IOException | RuntimeException e) {
            LOG.warn("Couldn't read {} for {}", source, getIconName(), e);
            image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }
        BufferedImage[] images = new BufferedImage[1 + Minecraft.getMinecraft().gameSettings.mipmapLevels];
        images[0] = SpriteFrames.variant(image, factor, variant);
        loadSprite(images, animation, Minecraft.getMinecraft().gameSettings.anisotropicFiltering > 1);
        return false;
    }
}
