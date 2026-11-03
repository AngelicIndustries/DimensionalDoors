package org.dimdev.dimdoors.client;

import java.awt.image.BufferedImage;

/** Pixel work for generated sprites: brightening, and turned copies that keep a noisy texture from tiling. */
public final class SpriteFrames {

    private SpriteFrames() {}

    /**
     * Copies an animation strip (square frames stacked top to bottom) with every frame brightened by factor, turned
     * variant & 3 quarter turns clockwise and mirrored from variant 4 up, starting variant frames later so copies don't
     * flicker in step.
     */
    public static BufferedImage variant(BufferedImage strip, float factor, int variant) {
        int size = strip.getWidth();
        int frames = Math.max(1, strip.getHeight() / size);
        BufferedImage out = new BufferedImage(size, size * frames, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < frames; frame++) {
            int from = (frame + variant) % frames;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    // Walk back from the output pixel to the one it shows: undo the mirror, then the turns
                    int sx = (variant & 4) != 0 ? size - 1 - x : x;
                    int sy = y;
                    for (int turn = 0; turn < (variant & 3); turn++) {
                        int t = sx;
                        sx = sy;
                        sy = size - 1 - t;
                    }
                    out.setRGB(x, frame * size + y, brighten(strip.getRGB(sx, from * size + sy), factor));
                }
            }
        }
        return out;
    }

    static int brighten(int color, float factor) {
        int r = Math.min(255, Math.round((color >> 16 & 255) * factor));
        int g = Math.min(255, Math.round((color >> 8 & 255) * factor));
        int b = Math.min(255, Math.round((color & 255) * factor));
        return color & 0xFF000000 | r << 16 | g << 8 | b;
    }
}
