package org.dimdev.dimdoors.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dimdev.dimdoors.blocks.TransTrapdoor;

/**
 * An item icon with the dimensional portal showing through: a door's or trapdoor's own icon with the portal moving in
 * its windows, as modern versions draw a door variant over the portal. One without windows keeps its plain icon. Made
 * when the textures are stitched, so it follows resource packs.
 */
public class PortalSprite extends TextureAtlasSprite {

    private static final Logger LOG = LogManager.getLogger("DimDoors");
    private static final ResourceLocation PORTAL = new ResourceLocation("dimdoors", "textures/door_variant_portal.png");
    private static final int FRAME_TIME = 2;

    private final Supplier<IIcon> base;
    private final String basePath;
    private boolean failed;

    /**
     * @param base     the plain icon, asked for when the atlas loads
     * @param basePath the atlas' texture folder, textures/items or textures/blocks
     */
    private PortalSprite(String name, Supplier<IIcon> base, String basePath) {
        super(name);
        this.base = base;
        this.basePath = basePath;
    }

    /** A door item's icon, drawn from the base door item's. */
    public static PortalSprite door(String name, ItemStack base, String basePath) {
        return new PortalSprite(
            name,
            () -> base.getItem()
                .getIconIndex(base),
            basePath);
    }

    /** A trapdoor item's icon, drawn from the block's plain icon in the block atlas, as the item is drawn in 3D. */
    public static PortalSprite trapdoor(String name, TransTrapdoor trapdoor) {
        return new PortalSprite(name, () -> trapdoor.plainIcon(1, 0), "textures/blocks");
    }

    /** Whether the icon couldn't be made, so the base door's icon should be drawn instead. */
    public boolean failed() {
        return failed;
    }

    @Override
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return true;
    }

    /** Returns false once loaded, as the texture map expects of a custom loader. */
    @Override
    public boolean load(IResourceManager manager, ResourceLocation location) {
        BufferedImage icon;
        boolean[] windows;
        BufferedImage portal = null;
        try {
            BufferedImage door = read(
                manager,
                iconLocation(
                    base.get()
                        .getIconName()));
            icon = door.getSubimage(0, 0, door.getWidth(), door.getWidth());
            windows = windows(icon);
            if (any(windows)) portal = read(manager, PORTAL);
        } catch (IOException | RuntimeException e) {
            LOG.warn("Couldn't draw the item icon of {}; it uses its plain one", getIconName(), e);
            failed = true;
            icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            windows = new boolean[16 * 16];
        }
        BufferedImage[] images = new BufferedImage[1 + Minecraft.getMinecraft().gameSettings.mipmapLevels];
        boolean anisotropic = Minecraft.getMinecraft().gameSettings.anisotropicFiltering > 1;
        if (portal == null) {
            images[0] = icon;
            loadSprite(images, null, anisotropic);
        } else {
            images[0] = animate(icon, windows, portal);
            loadSprite(images, new AnimationMetadataSection(new ArrayList<>(), -1, -1, FRAME_TIME), anisotropic);
        }
        return false;
    }

    private ResourceLocation iconLocation(String iconName) {
        ResourceLocation name = new ResourceLocation(iconName);
        return new ResourceLocation(name.getResourceDomain(), basePath + "/" + name.getResourcePath() + ".png");
    }

    private static BufferedImage read(IResourceManager manager, ResourceLocation location) throws IOException {
        try (InputStream in = manager.getResource(location)
            .getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) throw new IOException("Not an image: " + location);
            return image;
        }
    }

    /**
     * The door's windows: see-through pixels that the outside can't reach without crossing the door, so not the
     * space around a door's outline.
     */
    private static boolean[] windows(BufferedImage icon) {
        int size = icon.getWidth();
        boolean[] outside = new boolean[size * size];
        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < size; i++) {
            queue.add(i);
            queue.add((size - 1) * size + i);
            queue.add(i * size);
            queue.add(i * size + size - 1);
        }
        while (!queue.isEmpty()) {
            int p = queue.poll();
            int x = p % size, y = p / size;
            if (outside[p] || alpha(icon.getRGB(x, y)) != 0) continue;
            outside[p] = true;
            if (x > 0) queue.add(p - 1);
            if (x < size - 1) queue.add(p + 1);
            if (y > 0) queue.add(p - size);
            if (y < size - 1) queue.add(p + size);
        }
        boolean[] windows = new boolean[size * size];
        for (int y = 1; y < size - 1; y++) {
            for (int x = 1; x < size - 1; x++) {
                int p = y * size + x;
                // Pixels on the outline stay as they are, so the door's edge doesn't take the portal's colors
                windows[p] = !outside[p] && alpha(icon.getRGB(x, y)) < 255
                    && !outside[p - 1]
                    && !outside[p + 1]
                    && !outside[p - size]
                    && !outside[p + size];
            }
        }
        return windows;
    }

    /** One frame per portal frame, the portal drawn under the door's windows. */
    private static BufferedImage animate(BufferedImage icon, boolean[] windows, BufferedImage portal) {
        int size = icon.getWidth();
        int portalSize = portal.getWidth();
        int frames = Math.max(1, portal.getHeight() / portalSize);
        int[] box = opaqueBox(portal, portalSize);
        BufferedImage strip = new BufferedImage(size, size * frames, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < frames; frame++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int color = icon.getRGB(x, y);
                    if (windows[y * size + x]) {
                        // The portal is door-shaped; a window outside that shape takes its nearest edge
                        int px = clamp(x * portalSize / size, box[0], box[2]);
                        int py = clamp(y * portalSize / size, box[1], box[3]);
                        color = over(color, portal.getRGB(px, frame * portalSize + py));
                    }
                    strip.setRGB(x, frame * size + y, color);
                }
            }
        }
        return strip;
    }

    /** The first frame's opaque area: min x, min y, max x, max y. */
    private static int[] opaqueBox(BufferedImage portal, int size) {
        int[] box = { size - 1, size - 1, 0, 0 };
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (alpha(portal.getRGB(x, y)) == 0) continue;
                box[0] = Math.min(box[0], x);
                box[1] = Math.min(box[1], y);
                box[2] = Math.max(box[2], x);
                box[3] = Math.max(box[3], y);
            }
        }
        return box[0] > box[2] ? new int[] { 0, 0, size - 1, size - 1 } : box;
    }

    /** The door's pixel over the portal's. */
    private static int over(int door, int portal) {
        int a = alpha(door);
        int r = ((door >> 16 & 255) * a + (portal >> 16 & 255) * (255 - a)) / 255;
        int g = ((door >> 8 & 255) * a + (portal >> 8 & 255) * (255 - a)) / 255;
        int b = ((door & 255) * a + (portal & 255) * (255 - a)) / 255;
        int alpha = Math.max(a, alpha(portal));
        return alpha << 24 | r << 16 | g << 8 | b;
    }

    private static boolean any(boolean[] values) {
        for (boolean value : values) if (value) return true;
        return false;
    }

    private static int alpha(int color) {
        return color >>> 24;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
