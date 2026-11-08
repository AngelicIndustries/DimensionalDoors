package org.dimdev.dimdoors.client;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws a door's or trapdoor's portal in its shape (PortalShape): 16 layers of the warp texture, as vanilla draws the
 * End portal's starfield, then the glowing rim. Each layer's texture coordinates are worked out per vertex as the old
 * texture generation made them, since shader packs ignore generated coordinates (vanilla's End portal has the same
 * problem under Angelica). Their depth coordinate is the same all over the portal's plane, so dividing per vertex
 * gives the same picture.
 */
@SideOnly(Side.CLIENT)
public abstract class PortalRenderer extends TileEntitySpecialRenderer {

    protected static final ResourceLocation WARP = new ResourceLocation(DimDoors.modid + ":textures/other/WARP.png");
    private static final ResourceLocation RIM_TEXTURE = new ResourceLocation(
        DimDoors.modid + ":textures/other/portalRim.png");
    private static final int NETHER_DIMENSION_ID = -1;
    /** The rim's colour, and in the Nether, where the portal is red. */
    private static final float[] RIM = { 0.45F, 0.8F, 1.0F }, NETHER_RIM = { 1.0F, 0.25F, 0.08F };
    /** How many times the rim texture runs around the portal, and how fast it flows, in turns a second. */
    private static final int RIM_REPEAT = 3;
    private static final double RIM_FLOW = 0.05;

    private final PortalShape shape = new PortalShape();
    /** Per fill vertex: where it is drawn, and its texture coordinates before each layer's turn and scale. */
    private double[] drawn = new double[0], st = new double[0];
    private final double[] spot = new double[3], stq = new double[3];

    /** Where the spot h across and v up the portal's plane (from its middle) is drawn, into out from at. */
    protected abstract void place(TileEntity tile, double x, double y, double z, double h, double v, double[] out,
        int at);

    /** The s, t and q the old texture generation gave a spot (r isn't used by a 2D texture). */
    protected abstract void generatedCoords(TileEntity tile, double vx, double vy, double vz, float time, double[] stq);

    /** A layer's red, green and blue, drawn from the random as the old renderer did. */
    protected abstract float[] layerColor(TileEntity tile, int layer, Random random);

    /**
     * The portal in an opening from left to right and bottom to top, across and up its plane from the middle. Its
     * layers are drawn with the world and its rim afterwards, with what is see-through (tile entities get a second
     * pass after water): drawn with the world, the rim hid the water behind it, and under a shader pack the
     * background it glows over was lit and fogged as if it were at the portal. The first layer has its own texture;
     * the others dim the further back they are, by 1 / (depth + dimming).
     */
    protected void renderPortal(TileEntity tile, double x, double y, double z, double left, double right, double bottom,
        double top, ResourceLocation firstLayer, float dimming) {
        int pass = MinecraftForgeClient.getRenderPass();
        long now = Minecraft.getSystemTime();
        DDProperties properties = DDProperties.instance();
        shape.update(
            tile.xCoord * 3129871L ^ tile.zCoord * 116129781L ^ tile.yCoord,
            now,
            left,
            right,
            bottom,
            top,
            properties.PortalRoundness,
            properties.PortalEdgeRoughness,
            properties.PortalSpinSpeed);
        boolean shaders = ShaderPacks.inUse();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        if (pass != 1) renderLayers(tile, x, y, z, now, firstLayer, dimming, shaders);
        if (pass != 0) renderRim(tile, x, y, z, now, properties.PortalSpinSpeed, shaders);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    /** The 16 layers of the warp texture, in the portal's shape. */
    private void renderLayers(TileEntity tile, double x, double y, double z, long now, ResourceLocation firstLayer,
        float dimming, boolean shaders) {
        float time = now % 200000L / 200000.0F;
        Tessellator tessellator = Tessellator.instance;
        PortalShape.Quads fill = shape.fill;
        int count = fill.vertices();
        if (drawn.length < count * 3) {
            drawn = new double[count * 3];
            st = new double[count * 2];
        }
        for (int i = 0; i < count; i++) {
            place(tile, x, y, z, fill.get(i, 2), fill.get(i, 3), drawn, i * 3);
            place(tile, x, y, z, fill.get(i, 0), fill.get(i, 1), spot, 0);
            generatedCoords(tile, spot[0], spot[1], spot[2], time, stq);
            double q = Math.abs(stq[2]) < 1.0E-4 ? Math.copySign(1.0E-4, stq[2]) : stq[2];
            st[i * 2] = stq[0] / q + 0.5;
            st[i * 2 + 1] = stq[1] / q + 0.5;
        }
        Random random = new Random(31100L);
        for (int layer = 0; layer < 16; ++layer) {
            float speed = 16 - layer;
            float scale = layer == 1 ? .5F : 0.2625F;
            float brightness = (1.0F + shape.flash) / (speed + dimming);
            if (layer == 0) {
                brightness = 0.1F;
                speed = 25.0F;
                scale = 0.125F;
                bindTexture(firstLayer);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            } else if (layer == 1) {
                bindTexture(WARP);
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            }
            double angle = Math.toRadians((layer * layer * 4321 + layer * 9) * 2.0F);
            double cos = Math.cos(angle), sin = Math.sin(angle);
            float[] color = layerColor(tile, layer, random);
            tessellator.startDrawingQuads();
            if (shaders) tessellator.setBrightness(240 | 240 << 16);
            tessellator.setColorRGBA_F(color[0] * brightness, color[1] * brightness, color[2] * brightness, 1.0F);
            for (int i = 0; i < count; i++) {
                double s = st[i * 2], t = st[i * 2 + 1];
                double u = scale * (cos * s - sin * t + 0.5);
                double v = scale * (sin * s + cos * t + 0.5) + time * speed;
                tessellator.addVertexWithUV(drawn[i * 3], drawn[i * 3 + 1], drawn[i * 3 + 2], u, v);
            }
            tessellator.draw();
        }
    }

    /**
     * The rim, added on top without hiding what is behind it: brightest along the edge, its texture flowing around the
     * portal.
     */
    private void renderRim(TileEntity tile, double x, double y, double z, long now, double spinSpeed, boolean shaders) {
        bindTexture(RIM_TEXTURE);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glDepthMask(false);
        float[] color = tile.getWorldObj().provider.dimensionId == NETHER_DIMENSION_ID ? NETHER_RIM : RIM;
        double flow = now % 1000000L / 1000.0 * spinSpeed * RIM_FLOW * RIM_REPEAT;
        PortalShape.Quads rim = shape.rim;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        if (shaders) tessellator.setBrightness(240 | 240 << 16);
        for (int i = 0; i < rim.vertices(); i++) {
            float glow = (float) rim.get(i, 4) * (1.0F + shape.flash);
            tessellator.setColorRGBA_F(color[0] * glow, color[1] * glow, color[2] * glow, 1.0F);
            place(tile, x, y, z, rim.get(i, 2), rim.get(i, 3), spot, 0);
            tessellator.addVertexWithUV(spot[0], spot[1], spot[2], rim.get(i, 5) * RIM_REPEAT - flow, rim.get(i, 6));
        }
        tessellator.draw();
        GL11.glDepthMask(true);
    }
}
