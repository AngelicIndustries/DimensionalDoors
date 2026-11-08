package org.dimdev.dimdoors.client;

import static org.lwjgl.opengl.GL11.*;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.tileentities.TileEntityDimDoor;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderDimDoor extends TileEntitySpecialRenderer {

    private final ResourceLocation warpPath = new ResourceLocation(DimDoors.modid + ":textures/other/WARP.png");
    private final ResourceLocation rimPath = new ResourceLocation(DimDoors.modid + ":textures/other/portalRim.png");
    private final ResourceLocation keyPath = new ResourceLocation(DimDoors.modid + ":textures/other/keyhole.png");
    private final ResourceLocation KeyholeLight = new ResourceLocation(
        DimDoors.modid + ":textures/other/keyholeLight.png");
    private final ResourceLocation keyOutline = new ResourceLocation(DimDoors.modid + ":textures/other/keyOutline.png");
    private final ResourceLocation keyOutlineLight = new ResourceLocation(
        DimDoors.modid + ":textures/other/keyOutlineLight.png");

    private static final int NETHER_DIMENSION_ID = -1;
    /** The rim's colour, and in the Nether, where the portal is red. */
    private static final float[] RIM = { 0.45F, 0.8F, 1.0F }, NETHER_RIM = { 1.0F, 0.25F, 0.08F };
    /** How many times the rim texture runs around the oval, and how fast it flows, in turns a second. */
    private static final int RIM_REPEAT = 3;
    private static final double RIM_FLOW = 0.05;
    private static DDProperties properties = null;

    private final PortalShape shape = new PortalShape();
    /** Per fill vertex: where it is drawn, and its texture coordinates before each layer's turn and scale. */
    private double[] drawn = new double[0], st = new double[0];
    private final double[] spot = new double[3], stq = new double[3];

    public RenderDimDoor() {
        if (properties == null) properties = DDProperties.instance();
    }

    /**
     * The doorway: 16 layers of the warp texture, as vanilla draws the End portal's starfield, in the portal's shape
     * (PortalShape), an oval, also behind a shut door's windows. Each layer's
     * texture coordinates are worked out per vertex as texture generation would make them, since shader packs ignore
     * generated coordinates (vanilla's End portal has the same problem under Angelica). Their depth coordinate only
     * depends on the axis the doorway faces, so it is the same at every vertex and dividing per vertex gives the same
     * picture. Then the portal's rim glows on top.
     */
    private void renderDoorway(TileEntityDimDoor tile, double x, double y, double z) {
        long now = Minecraft.getSystemTime();
        shape.oval(seed(tile), now, 0.5, 1);
        boolean shaders = ShaderPacks.inUse();
        float time = now % 200000L / 200000.0F;
        this.bindTexture(warpPath);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        Tessellator tessellator = Tessellator.instance;

        PortalShape.Quads fill = shape.fill;
        int count = fill.vertices();
        if (drawn.length < count * 3) {
            drawn = new double[count * 3];
            st = new double[count * 2];
        }
        for (int i = 0; i < count; i++) {
            place(tile.orientation, x, y, z, fill.get(i, 2), fill.get(i, 3), drawn, i * 3);
            place(tile.orientation, x, y, z, fill.get(i, 0), fill.get(i, 1), spot, 0);
            generatedCoords(tile.orientation, spot[0], spot[1], spot[2], stq);
            double q = Math.abs(stq[2]) < 1.0E-4 ? Math.copySign(1.0E-4, stq[2]) : stq[2];
            st[i * 2] = stq[0] / q + 0.5;
            st[i * 2 + 1] = stq[1] / q + 0.5;
        }
        Random rand = new Random(31100L);
        for (int layer = 0; layer < 16; ++layer) {
            float speed = 16 - layer;
            float scale = layer == 1 ? .5F : 0.2625F;
            float brightness = (1.0F + shape.flash) / (speed + .80F);
            if (layer == 0) {
                brightness = 0.1F;
                speed = 25.0F;
                scale = 0.125F;
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            } else if (layer == 1) {
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            }
            double angle = Math.toRadians((layer * layer * 4321 + layer * 9) * 2.0F);
            double cos = Math.cos(angle), sin = Math.sin(angle);
            float[] color = tile.getRenderColor(rand);
            tessellator.startDrawingQuads();
            if (shaders) tessellator.setBrightness(240 | 240 << 16);
            tessellator.setColorRGBA_F(color[0] * brightness, color[1] * brightness, color[2] * brightness, color[3]);
            for (int i = 0; i < count; i++) {
                double s = st[i * 2], t = st[i * 2 + 1];
                double u = scale * (cos * s - sin * t + 0.5);
                double v = scale * (sin * s + cos * t + 0.5) + time * speed;
                tessellator.addVertexWithUV(drawn[i * 3], drawn[i * 3 + 1], drawn[i * 3 + 2], u, v);
            }
            tessellator.draw();
        }
        if (shape.rim.vertices() > 0) renderRim(tile, x, y, z, now, shaders);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    /** The portal's rim, added on top: brightest along the edge, its texture flowing around the oval. */
    private void renderRim(TileEntityDimDoor tile, double x, double y, double z, long now, boolean shaders) {
        this.bindTexture(rimPath);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        float[] color = tile.getWorldObj().provider.dimensionId == NETHER_DIMENSION_ID ? NETHER_RIM : RIM;
        double flow = now % 1000000L / 1000.0 * RIM_FLOW * RIM_REPEAT;
        PortalShape.Quads rim = shape.rim;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        if (shaders) tessellator.setBrightness(240 | 240 << 16);
        for (int i = 0; i < rim.vertices(); i++) {
            float glow = (float) rim.get(i, 4) * (1.0F + shape.flash);
            tessellator.setColorRGBA_F(color[0] * glow, color[1] * glow, color[2] * glow, 1.0F);
            place(tile.orientation, x, y, z, rim.get(i, 2), rim.get(i, 3), spot, 0);
            tessellator.addVertexWithUV(spot[0], spot[1], spot[2], rim.get(i, 5) * RIM_REPEAT - flow, rim.get(i, 6));
        }
        tessellator.draw();
    }

    /** Each door's own portal shape. */
    private static long seed(TileEntity tile) {
        return tile.xCoord * 3129871L ^ tile.zCoord * 116129781L ^ tile.yCoord;
    }

    /**
     * A spot on the doorway's plane: h across from its middle and v up from where the halves meet. Across runs so the
     * portal's front faces the same side as the doorway's one quad did.
     */
    private static void place(int orientation, double x, double y, double z, double h, double v, double[] out, int at) {
        switch (orientation) {
            case 0:
            case 4:
                out[at] = x + (orientation == 0 ? .01 : .15);
                out[at + 2] = z + 0.5 + h;
                break;
            case 1:
            case 5:
                out[at] = x + 0.5 - h;
                out[at + 2] = z + (orientation == 1 ? .01 : .15);
                break;
            case 2:
            case 6:
                out[at] = x + (orientation == 2 ? .99 : .85);
                out[at + 2] = z + 0.5 - h;
                break;
            default:
                out[at] = x + 0.5 + h;
                out[at + 2] = z + (orientation == 3 ? .99 : .85);
        }
        out[at + 1] = y + v;
    }

    /** The s, t and q that texture generation's planes give a spot (r isn't used by a 2D texture). */
    private static void generatedCoords(int orientation, double vx, double vy, double vz, double[] stq) {
        boolean acrossZ = orientation % 2 == 0;
        stq[0] = vy;
        stq[1] = acrossZ ? vz : vx;
        stq[2] = (acrossZ ? vx : vz) + (orientation % 4 < 2 ? 0.15 : -0.15);
    }

    private void renderKeyHole(TileEntityDimDoor tile, double x, double y, double z, int i) {
        if (tile.orientation > 3) {
            return;
        }
        int rotation = (tile.orientation + 3) % 4;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        x = ActiveRenderInfo.objectX;
        y = ActiveRenderInfo.objectY;
        z = ActiveRenderInfo.objectZ;

        GL11.glRotatef(180.0F - 90 * rotation, 0.0F, 1.0F, 0.0F);
        // GL11.glRotatef((float)(-90 * rotation), 0.0F, 0.0F, 1.0F);

        GL11.glTranslatef(0.007F, .25F, 0F);

        switch (rotation) {
            case 0:
                GL11.glTranslatef(-0.5F, 0, -0.03F);
                break;
            case 1:
                GL11.glTranslatef(-.5F, 0F, .97F);
                break;
            case 2:
                GL11.glTranslatef(.5F, 0F, .97F);
                break;
            case 3:
                GL11.glTranslatef(0.5F, 0F, -0.03F);
        }

        GL11.glDisable(GL_LIGHTING);
        Tessellator tessellator = Tessellator.instance;
        GL11.glEnable(GL11.GL_BLEND);
        if (i == 1) {
            bindTexture(KeyholeLight);
            GL11.glColor4d(1, 1, 1, .7);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_DST_COLOR);

        } else {
            bindTexture(keyPath);
            glBlendFunc(GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA);

        }
        GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
        GL11.glScalef(0.00860625F, 0.00730625F, 0.0086625F);
        GL11.glTranslatef(-65.0F, -107.0F, -3.0F);
        GL11.glNormal3f(0.0F, 0.0F, -1.0F);
        tessellator.startDrawingQuads();
        byte b0 = 7;
        tessellator.addVertexWithUV(-b0, 128 + b0, 0.0D, 0.0D, 1.0D);
        tessellator.addVertexWithUV(128 + b0, 128 + b0, 0.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(128 + b0, -b0, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(-b0, -b0, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
        GL11.glTranslatef(0.0F, 0.0F, -1.0F);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glPopMatrix();
    }

    @Override
    public void renderTileEntityAt(TileEntity par1TileEntity, double par2, double par4, double par6, float par8) {
        if (properties.DoorRenderingEnabled) {
            TileEntityDimDoor tile = (TileEntityDimDoor) par1TileEntity;
            try {
                DimDoors.dimensionalDoor.updateAttachedTile(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (tile.openOrClosed) {

                renderDoorway(tile, par2, par4, par6);
                if (tile.lockStatus >= 1) {
                    for (int i = 0; i < 1 + tile.lockStatus; i++) {
                        this.renderKeyHole(tile, par2, par4, par6, i);

                    }
                }

            }

        }
    }
}
