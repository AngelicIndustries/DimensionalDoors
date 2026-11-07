package org.dimdev.dimdoors.client;

import net.minecraft.client.renderer.Tessellator;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A spinning four-dimensional cube, the core a rift shows while it's highlighted. From the 1.12 rewrite (MIT): each
 * face turns in the y-w plane and is projected down to three dimensions.
 */
@SideOnly(Side.CLIENT)
public final class Tesseract {

    // The 24 square faces, four corners each, x y z w with + for 0.5 and - for -0.5, in 1.12's order
    private static final String[] FACES = { "---- +--- +-+- --+-", "-+-- ++-- +++- -++-", "---- +--- ++-- -+--",
        "--+- +-+- +++- -++-", "---- -+-- -++- --+-", "+--- ++-- +++- +-+-", "---+ +--+ +-++ --++",
        "-+-+ ++-+ ++++ -+++", "---+ +--+ ++-+ -+-+", "--++ +-++ ++++ -+++", "---+ -+-+ -+++ --++",
        "+--+ ++-+ ++++ +-++", "---- -+-- -+-+ ---+", "--+- -++- -+++ --++", "---- -+-- -++- --+-",
        "---+ -+-+ -+++ --++", "---- --+- --++ ---+", "-+-- -++- -+++ -+-+", "+--- ++-- ++-+ +--+",
        "+-+- +++- ++++ +-++", "+--- ++-- +++- +-+-", "+--+ ++-+ ++++ +-++", "+--- +-+- +-++ +--+",
        "++-- +++- ++++ ++-+" };
    private static final int[][] UV = { { 0, 0 }, { 0, 1 }, { 1, 1 }, { 1, 0 } };
    private static final float[][][] CORNERS = new float[FACES.length][4][4];

    static {
        for (int f = 0; f < FACES.length; f++) {
            String[] corners = FACES[f].split(" ");
            for (int c = 0; c < 4; c++)
                for (int k = 0; k < 4; k++) CORNERS[f][c][k] = corners[c].charAt(k) == '+' ? 0.5F : -0.5F;
        }
    }

    private Tesseract() {}

    /** Draws it around the origin, turned by angle (radians), at full brightness, with the texture that's bound. */
    public static void draw(float red, float green, float blue, float alpha, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setBrightness(240 | 240 << 16);
        tessellator.setColorRGBA_F(red, green, blue, alpha);
        for (float[][] face : CORNERS) {
            for (int c = 0; c < 4; c++) {
                float[] v = face[c];
                // Turn in the y-w plane, then project: the nearer in w, the bigger
                double y = cos * v[1] + sin * v[3];
                double w = -sin * v[1] + cos * v[3];
                double scale = 1 / (w + 1);
                tessellator.addVertexWithUV(v[0] * scale, y * scale, v[2] * scale, UV[c][0], UV[c][1]);
            }
        }
        tessellator.draw();
    }
}
