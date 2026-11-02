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
 * draws spider eyes). Made from the texture as loaded, so it follows resource packs.
 */
public class MonolithEyesTexture extends AbstractTexture {

    private final ResourceLocation source;

    private MonolithEyesTexture(ResourceLocation source) {
        this.source = source;
    }

    /** The eyes of a Monolith texture, loaded the first time they are asked for. */
    public static ResourceLocation of(ResourceLocation monolithTexture) {
        ResourceLocation eyes = new ResourceLocation(
            monolithTexture.getResourceDomain(),
            monolithTexture.getResourcePath() + "#eyes");
        TextureManager textures = Minecraft.getMinecraft()
            .getTextureManager();
        ITextureObject loaded = textures.getTexture(eyes);
        if (loaded == null) textures.loadTexture(eyes, new MonolithEyesTexture(monolithTexture));
        return eyes;
    }

    @Override
    public void loadTexture(IResourceManager manager) throws IOException {
        deleteGlTexture();
        BufferedImage image;
        try (InputStream in = manager.getResource(source)
            .getInputStream()) {
            image = ImageIO.read(in);
        }
        BufferedImage eyes = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int color = image.getRGB(x, y);
                int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
                // The eyes are the texture's red pixels; everything else stays out of the glow
                if (color >>> 24 > 0 && r > 100 && r > g * 2 && r > b * 2) eyes.setRGB(x, y, color);
            }
        }
        TextureUtil.uploadTextureImageAllocate(getGlTextureId(), eyes, false, false);
    }
}
