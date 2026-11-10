package org.dimdev.dimdoors.client;

import static org.lwjgl.opengl.GL11.*;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.config.DDProperties.PortalPlacement;
import org.dimdev.dimdoors.tileentities.TileEntityDimDoor;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A dimensional door's portal in its doorway (also behind a shut door's windows), placed around an open door's panel
 * as the config says, and its keyhole.
 */
@SideOnly(Side.CLIENT)
public class RenderDimDoor extends PortalRenderer {

    private final ResourceLocation keyPath = new ResourceLocation(DimDoors.modid + ":textures/other/keyhole.png");
    private final ResourceLocation KeyholeLight = new ResourceLocation(
        DimDoors.modid + ":textures/other/keyholeLight.png");
    private final ResourceLocation keyOutline = new ResourceLocation(DimDoors.modid + ":textures/other/keyOutline.png");
    private final ResourceLocation keyOutlineLight = new ResourceLocation(
        DimDoors.modid + ":textures/other/keyOutlineLight.png");

    private static DDProperties properties = null;
    /** How thick a door's or trapdoor's panel is. */
    static final double PANEL = 0.1875;
    /** How far into its block, along the way it faces, the portal of the door being drawn lies. */
    private double depth;

    public RenderDimDoor() {
        if (properties == null) properties = DDProperties.instance();
    }

    /**
     * A spot on the doorway's plane: h across from its middle and v up from where the halves meet. Across runs so the
     * portal's front faces the same side as the doorway's one quad did.
     */
    @Override
    protected void place(TileEntity tile, double x, double y, double z, double h, double v, double[] out, int at) {
        switch (((TileEntityDimDoor) tile).orientation % 4) {
            case 0:
                out[at] = x + depth;
                out[at + 2] = z + 0.5 + h;
                break;
            case 1:
                out[at] = x + 0.5 - h;
                out[at + 2] = z + depth;
                break;
            case 2:
                out[at] = x + depth;
                out[at + 2] = z + 0.5 - h;
                break;
            default:
                out[at] = x + 0.5 + h;
                out[at + 2] = z + depth;
        }
        out[at + 1] = y + v;
    }

    /** Object planes: s is the height, t runs across the doorway, q is the distance to a plane by its depth. */
    @Override
    protected void generatedCoords(TileEntity tile, double vx, double vy, double vz, float time, double[] stq) {
        int orientation = ((TileEntityDimDoor) tile).orientation;
        boolean acrossZ = orientation % 2 == 0;
        stq[0] = vy;
        stq[1] = acrossZ ? vz : vx;
        stq[2] = (acrossZ ? vx : vz) + (orientation % 4 < 2 ? 0.15 : -0.15);
    }

    @Override
    protected float[] layerColor(TileEntity tile, int layer, Random random) {
        return ((TileEntityDimDoor) tile).getRenderColor(random);
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
        ClosingRifts.draw(par1TileEntity, par2, par4, par6);
        if (properties.DoorRenderingEnabled) {
            TileEntityDimDoor tile = (TileEntityDimDoor) par1TileEntity;
            try {
                DimDoors.dimensionalDoor.updateAttachedTile(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (tile.openOrClosed) {
                // The portal faces out of the block's low side for orientations 0 and 1, its high side for 2 and 3:
                // inside a shut door's panel, set into an open doorway, or just in front of it
                boolean low = tile.orientation % 4 < 2;
                PortalPlacement placement = properties.OpenPortalPlacement;
                if (tile.orientation < 4) depth = low ? .01 : .99;
                else if (placement == PortalPlacement.FORWARD) depth = low ? -.01 : 1.01;
                else depth = low ? .15 : .85;
                // An open door's panel stands in the doorway along its hinge side: the left, seen from the portal's
                // front, unless the door is hinged the other way
                double left = -0.5, right = 0.5;
                if (tile.orientation > 3 && placement == PortalPlacement.NARROW) {
                    Block block = tile.getWorldObj()
                        .getBlock(tile.xCoord, tile.yCoord, tile.zCoord);
                    boolean mirrored = block instanceof BlockDoor
                        && (((BlockDoor) block).func_150012_g(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord)
                            & 16) != 0;
                    if (mirrored) right -= PANEL;
                    else left += PANEL;
                }
                renderPortal(tile, par2, par4, par6, left, right, -1, 1, WARP, .80F);
                if (tile.lockStatus >= 1 && MinecraftForgeClient.getRenderPass() != 1) {
                    for (int i = 0; i < 1 + tile.lockStatus; i++) {
                        this.renderKeyHole(tile, par2, par4, par6, i);

                    }
                }

            }

        }
    }
}
