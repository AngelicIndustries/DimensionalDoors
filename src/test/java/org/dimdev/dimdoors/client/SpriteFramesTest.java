package org.dimdev.dimdoors.client;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;

import org.junit.Test;

public class SpriteFramesTest {

    private static final int SIZE = 4;

    /** Two frames; every pixel's colour encodes its frame and position. */
    private static BufferedImage strip() {
        BufferedImage strip = new BufferedImage(SIZE, SIZE * 2, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < 2; frame++) for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++) strip.setRGB(x, frame * SIZE + y, pixel(frame, x, y));
        return strip;
    }

    private static int pixel(int frame, int x, int y) {
        return 0xFF000000 | frame << 16 | x << 8 | y;
    }

    @Test
    public void variantZeroIsACopy() {
        BufferedImage out = SpriteFrames.variant(strip(), 1.0F, 0);
        for (int frame = 0; frame < 2; frame++) for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++) assertEquals(pixel(frame, x, y), out.getRGB(x, frame * SIZE + y));
    }

    @Test
    public void oneTurnIsClockwiseAndStartsAFrameLater() {
        BufferedImage out = SpriteFrames.variant(strip(), 1.0F, 1);
        // The source's top left corner ends up top right, its top right bottom right
        assertEquals(pixel(1, 0, 0), out.getRGB(SIZE - 1, 0));
        assertEquals(pixel(1, SIZE - 1, 0), out.getRGB(SIZE - 1, SIZE - 1));
        assertEquals(pixel(0, 0, 0), out.getRGB(SIZE - 1, SIZE));
    }

    @Test
    public void fromFourUpItIsMirrored() {
        BufferedImage out = SpriteFrames.variant(strip(), 1.0F, 4);
        assertEquals(pixel(0, 0, 2), out.getRGB(SIZE - 1, 2));
    }

    @Test
    public void brighteningScalesAndClampsButKeepsAlpha() {
        assertEquals(0x80406080, SpriteFrames.brighten(0x80203040, 2.0F));
        assertEquals(0xFFFFFF00, SpriteFrames.brighten(0xFFC0FF00, 2.0F));
    }
}
