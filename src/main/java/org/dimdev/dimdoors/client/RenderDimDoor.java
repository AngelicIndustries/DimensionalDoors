package org.dimdev.dimdoors.client;

import static org.lwjgl.opengl.GL11.*;

import java.nio.FloatBuffer;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GLAllocation;
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

    private final FloatBuffer buffer = GLAllocation.createDirectFloatBuffer(16);
    private final ResourceLocation warpPath = new ResourceLocation(DimDoors.modid + ":textures/other/WARP.png");
    private final ResourceLocation keyPath = new ResourceLocation(DimDoors.modid + ":textures/other/keyhole.png");
    private final ResourceLocation KeyholeLight = new ResourceLocation(
        DimDoors.modid + ":textures/other/keyholeLight.png");
    private final ResourceLocation keyOutline = new ResourceLocation(DimDoors.modid + ":textures/other/keyOutline.png");
    private final ResourceLocation keyOutlineLight = new ResourceLocation(
        DimDoors.modid + ":textures/other/keyOutlineLight.png");

    private static final int NETHER_DIMENSION_ID = -1;
    private static DDProperties properties = null;

    public RenderDimDoor() {
        if (properties == null) properties = DDProperties.instance();
    }

    /**
     * Renders the dimdoor.
     */
    public void renderDimDoorTileEntity(TileEntityDimDoor tile, double x, double y, double z) {

        GL11.glDisable(GL11.GL_LIGHTING);
        Random rand = new Random(31100L);
        float var13 = 0.75F;

        for (int count = 0; count < 16; ++count) {

            GL11.glPushMatrix();
            float var15 = 16 - count;
            float var16 = 0.2625F;
            float var17 = 1.0F / (var15 + .80F);

            if (count == 0) {
                this.bindTexture(warpPath);
                // move files into assets/modid and change to new ResourceLocation(modid:/RIFT.png)
                var17 = 0.1F;
                var15 = 25.0F;
                var16 = 0.125F;
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }

            if (count == 1) {
                this.bindTexture(warpPath);
                // move files into assets/modid and change to new ResourceLocation(modid:/WARP.png)
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                var16 = .5F;
            }

            GL11.glTranslatef(Minecraft.getSystemTime() % 200000L / 200000.0F, 0, 0.0F);
            GL11.glTranslatef(0, Minecraft.getSystemTime() % 200000L / 200000.0F, 0.0F);
            GL11.glTranslatef(0, 0, Minecraft.getSystemTime() % 200000L / 200000.0F);

            GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_OBJECT_LINEAR);
            GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_OBJECT_LINEAR);
            GL11.glTexGeni(GL11.GL_R, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_OBJECT_LINEAR);
            GL11.glTexGeni(GL11.GL_Q, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_OBJECT_LINEAR);
            switch ((tile.orientation % 4) + 4) {
                case 4:
                    GL11.glTexGen(GL11.GL_S, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
                    GL11.glTexGen(GL11.GL_T, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(0.0F, 0.0F, 1.0F, 0.0F));
                    GL11.glTexGen(GL11.GL_R, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
                    GL11.glTexGen(GL11.GL_Q, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(1.0F, 0.0F, 0.0F, 0.15F));

                    break;
                case 5:

                    GL11.glTexGen(GL11.GL_S, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
                    GL11.glTexGen(GL11.GL_T, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(1.0F, 0.0F, 0.0F, 0.0F));
                    GL11.glTexGen(GL11.GL_R, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
                    GL11.glTexGen(GL11.GL_Q, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(0.0F, 0.0F, 1.0F, 0.15F));
                    break;
                case 6:
                    GL11.glTexGen(GL11.GL_S, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
                    GL11.glTexGen(GL11.GL_T, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(0.0F, 0.0F, 1.0F, 0.0F));
                    GL11.glTexGen(GL11.GL_R, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
                    GL11.glTexGen(GL11.GL_Q, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(1.0F, 0.0F, 0.0F, -0.15F));

                    break;
                case 7:
                    GL11.glTexGen(GL11.GL_S, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
                    GL11.glTexGen(GL11.GL_T, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(1.0F, 0.0F, 0.0F, 0.0F));
                    GL11.glTexGen(GL11.GL_R, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
                    GL11.glTexGen(GL11.GL_Q, GL11.GL_OBJECT_PLANE, this.getFloatBuffer(0.0F, 0.0F, 1.0F, -0.15F));
                    break;

            }

            GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
            GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
            GL11.glEnable(GL11.GL_TEXTURE_GEN_R);
            GL11.glEnable(GL11.GL_TEXTURE_GEN_Q);

            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glTranslatef(0.0F, Minecraft.getSystemTime() % 200000L / 200000.0F * var15, 0.0F);
            GL11.glScalef(var16, var16, var16);
            GL11.glTranslatef(0.5F, 0.5F, 0.5F);
            GL11.glRotatef((count * count * 4321 + count * 9) * 2.0F, 0.0F, 0.0F, 1.0F);
            GL11.glTranslatef(0.5F, 0.5F, 0.5F);

            GL11.glBegin(GL11.GL_QUADS);

            float[] color = tile.getRenderColor(rand);
            GL11.glColor4f(color[0] * var17, color[1] * var17, color[2] * var17, color[3]);

            switch (tile.orientation) {
                case 0:

                    GL11.glVertex3d(x + .01F, y - 1, z);
                    GL11.glVertex3d(x + .01, y - 1, z + 1.0D);
                    GL11.glVertex3d(x + .01, y + 1, z + 1.0D);
                    GL11.glVertex3d(x + .01, y + 1, z);
                    break;
                case 1:
                    GL11.glVertex3d(x, y + 1, z + .01);
                    GL11.glVertex3d(x + 1, y + 1, z + .01);
                    GL11.glVertex3d(x + 1, y - 1, z + .01);
                    GL11.glVertex3d(x, y - 1, z + .01);
                    break;
                case 2:
                    GL11.glVertex3d(x + .99, y + 1, z);
                    GL11.glVertex3d(x + .99, y + 1, z + 1.0D);
                    GL11.glVertex3d(x + .99, y - 1, z + 1.0D);
                    GL11.glVertex3d(x + .99, y - 1, z);
                    break;
                case 3:
                    GL11.glVertex3d(x, y - 1, z + .99);
                    GL11.glVertex3d(x + 1, y - 1, z + .99);
                    GL11.glVertex3d(x + 1, y + 1, z + .99);
                    GL11.glVertex3d(x, y + 1, z + .99);
                    break;
                case 4:
                    GL11.glVertex3d(x + .15F, y - 1, z);
                    GL11.glVertex3d(x + .15, y - 1, z + 1.0D);
                    GL11.glVertex3d(x + .15, y + 1, z + 1.0D);
                    GL11.glVertex3d(x + .15, y + 1, z);
                    break;
                case 5:
                    GL11.glVertex3d(x, y + 1, z + .15);
                    GL11.glVertex3d(x + 1, y + 1, z + .15);
                    GL11.glVertex3d(x + 1, y - 1, z + .15);
                    GL11.glVertex3d(x, y - 1, z + .15);
                    break;
                case 6:
                    GL11.glVertex3d(x + .85, y + 1, z);
                    GL11.glVertex3d(x + .85, y + 1, z + 1.0D);
                    GL11.glVertex3d(x + .85, y - 1, z + 1.0D);
                    GL11.glVertex3d(x + .85, y - 1, z);
                    break;
                case 7:
                    GL11.glVertex3d(x, y - 1, z + .85);
                    GL11.glVertex3d(x + 1, y - 1, z + .85);
                    GL11.glVertex3d(x + 1, y + 1, z + .85);
                    GL11.glVertex3d(x, y + 1, z + .85);
                    break;
            }

            GL11.glEnd();

            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_S);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_R);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_Q);
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    /**
     * The doorway for shader packs, which ignore the generated texture coordinates the layers above use (vanilla's End
     * portal has the same problem under Angelica). The same 16 layers, worked out here instead: the generated
     * coordinates and the texture matrix for each corner. Their depth coordinate only depends on the axis the doorway
     * faces, so it is the same at all four corners and dividing per corner gives the same picture.
     */
    private void renderShaderDoorway(TileEntityDimDoor tile, double x, double y, double z) {
        Random rand = new Random(31100L);
        float time = Minecraft.getSystemTime() % 200000L / 200000.0F;
        this.bindTexture(warpPath);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        Tessellator tessellator = Tessellator.instance;
        double[][] corners = doorwayCorners(tile.orientation, x, y, z);
        for (int count = 0; count < 16; ++count) {
            // The same values as renderDimDoorTileEntity
            float var15 = 16 - count;
            float var16 = count == 1 ? .5F : 0.2625F;
            float var17 = 1.0F / (var15 + .80F);
            if (count == 0) {
                var17 = 0.1F;
                var15 = 25.0F;
                var16 = 0.125F;
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            } else if (count == 1) {
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            }
            double angle = Math.toRadians((count * count * 4321 + count * 9) * 2.0F);
            double cos = Math.cos(angle), sin = Math.sin(angle);
            float[] color = tile.getRenderColor(rand);
            tessellator.startDrawingQuads();
            tessellator.setBrightness(240 | 240 << 16);
            tessellator.setColorRGBA_F(color[0] * var17, color[1] * var17, color[2] * var17, color[3]);
            for (double[] corner : corners) {
                double[] stq = generatedCoords(tile.orientation, corner[0], corner[1], corner[2]);
                double q = Math.abs(stq[2]) < 1.0E-4 ? Math.copySign(1.0E-4, stq[2]) : stq[2];
                double s = stq[0] / q + 0.5, t = stq[1] / q + 0.5;
                double u = var16 * (cos * s - sin * t + 0.5);
                double v = var16 * (sin * s + cos * t + 0.5) + time * var15;
                tessellator.addVertexWithUV(corner[0], corner[1], corner[2], u, v);
            }
            tessellator.draw();
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    /** The doorway's corners, in the order the layers above draw them, so the same side shows. */
    private static double[][] doorwayCorners(int orientation, double x, double y, double z) {
        switch (orientation) {
            case 0:
            case 4:
                double px = x + (orientation == 0 ? .01 : .15);
                return new double[][] { { px, y - 1, z }, { px, y - 1, z + 1 }, { px, y + 1, z + 1 },
                    { px, y + 1, z } };
            case 1:
            case 5:
                double pz = z + (orientation == 1 ? .01 : .15);
                return new double[][] { { x, y + 1, pz }, { x + 1, y + 1, pz }, { x + 1, y - 1, pz },
                    { x, y - 1, pz } };
            case 2:
            case 6:
                double nx = x + (orientation == 2 ? .99 : .85);
                return new double[][] { { nx, y + 1, z }, { nx, y + 1, z + 1 }, { nx, y - 1, z + 1 },
                    { nx, y - 1, z } };
            default:
                double nz = z + (orientation == 3 ? .99 : .85);
                return new double[][] { { x, y - 1, nz }, { x + 1, y - 1, nz }, { x + 1, y + 1, nz },
                    { x, y + 1, nz } };
        }
    }

    /** The s, t and q the texture planes above give a corner (r isn't used by a 2D texture). */
    private static double[] generatedCoords(int orientation, double vx, double vy, double vz) {
        switch (orientation % 4) {
            case 0:
                return new double[] { vy, vz, vx + 0.15 };
            case 1:
                return new double[] { vy, vx, vz + 0.15 };
            case 2:
                return new double[] { vy, vz, vx - 0.15 };
            default:
                return new double[] { vy, vx, vz - 0.15 };
        }
    }

    private FloatBuffer getFloatBuffer(float par1, float par2, float par3, float par4) {
        buffer.clear();
        buffer.put(par1)
            .put(par2)
            .put(par3)
            .put(par4);
        buffer.flip();
        return buffer;
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

                if (ShaderPacks.inUse()) renderShaderDoorway(tile, par2, par4, par6);
                else renderDimDoorTileEntity((TileEntityDimDoor) par1TileEntity, par2, par4, par6);
                if (tile.lockStatus >= 1) {
                    for (int i = 0; i < 1 + tile.lockStatus; i++) {
                        this.renderKeyHole(tile, par2, par4, par6, i);

                    }
                }

            }

        }
    }
}
