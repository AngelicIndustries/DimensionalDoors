package org.dimdev.dimdoors.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

/**
 * Just the red eyes of a Monolith texture, to draw them again at full brightness so they glow in the dark (as vanilla
 * draws spider eyes), or as a grey mask, for their glow under a shader pack (MonolithEyeGlow). Made from the texture as
 * loaded, so it follows resource packs.
 */
public class MonolithEyesTexture extends AbstractTexture {

    private final ResourceLocation source;
    private final boolean mask;

    private MonolithEyesTexture(ResourceLocation source, boolean mask) {
        this.source = source;
        this.mask = mask;
    }

    /** The eyes of a Monolith texture, loaded the first time they are asked for. */
    public static ResourceLocation of(ResourceLocation monolithTexture) {
        return load(monolithTexture, false);
    }

    /** The eyes in shades of grey, to be tinted. */
    public static ResourceLocation mask(ResourceLocation monolithTexture) {
        return load(monolithTexture, true);
    }

    private static ResourceLocation load(ResourceLocation monolithTexture, boolean mask) {
        ResourceLocation texture = new ResourceLocation(
            monolithTexture.getResourceDomain(),
            monolithTexture.getResourcePath() + (mask ? "#mask" : "#eyes"));
        TextureManager textures = Minecraft.getMinecraft()
            .getTextureManager();
        ITextureObject loaded = textures.getTexture(texture);
        if (loaded == null) textures.loadTexture(texture, new MonolithEyesTexture(monolithTexture, mask));
        return texture;
    }

    @Override
    public void loadTexture(IResourceManager manager) throws IOException {
        deleteGlTexture();
        BufferedImage image;
        try (InputStream in = manager.getResource(source)
            .getInputStream()) {
            image = ImageIO.read(in);
        }
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int color = image.getRGB(x, y);
                int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
                // The eyes are the texture's red pixels
                if (color >>> 24 == 0 || r <= 100 || r <= g * 2 || r <= b * 2) continue;
                out.setRGB(x, y, mask ? color & 0xFF000000 | r << 16 | r << 8 | r : color);
            }
        }
        TextureUtil.uploadTextureImageAllocate(getGlTextureId(), out, false, false);
    }
}
