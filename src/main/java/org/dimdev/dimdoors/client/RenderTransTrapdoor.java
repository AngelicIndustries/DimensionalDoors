package org.dimdev.dimdoors.client;

import java.util.Random;

import net.minecraft.block.BlockTrapDoor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.blocks.TransTrapdoor;
import org.dimdev.dimdoors.config.DDProperties;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A transdimensional trapdoor's portal, lying in its opening (also under a shut trapdoor). */
@SideOnly(Side.CLIENT)
public class RenderTransTrapdoor extends PortalRenderer {

    private static DDProperties properties = null;
    private final ResourceLocation riftPath = new ResourceLocation(DimDoors.modid + ":textures/other/RIFT.png");
    /** How high in its block the portal lies, for the trapdoor being drawn. */
    private double height;

    public RenderTransTrapdoor() {
        if (properties == null) properties = DDProperties.instance();
    }

    /**
     * A spot on the portal: h east and v north from the opening's middle, so its front faces up as the old quad's did.
     */
    @Override
    protected void place(TileEntity tile, double x, double y, double z, double h, double v, double[] out, int at) {
        out[at] = x + 0.5 + h;
        out[at + 1] = y + height;
        out[at + 2] = z + 0.5 - v;
    }

    /**
     * Object planes for s and t (east and south), and an eye plane for q: the height above the camera, less the time
     * the old renderer had translated by when it set the plane.
     */
    @Override
    protected void generatedCoords(TileEntity tile, double vx, double vy, double vz, float time, double[] stq) {
        stq[0] = vx;
        stq[1] = vz;
        stq[2] = vy - time;
    }

    @Override
    protected float[] layerColor(TileEntity tile, int layer, Random random) {
        float red = random.nextFloat() * 0.5F + 0.1F;
        float green = random.nextFloat() * 0.4F + 0.4F;
        float blue = random.nextFloat() * 0.6F + 0.5F;
        return layer == 0 ? new float[] { red, 1.0F, 1.0F } : new float[] { red, green, blue };
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!properties.DoorRenderingEnabled) return;
        // As high as the old quad lay for the trapdoor's half and whether it is open
        int metadata = tile.getWorldObj()
            .getBlockMetadata(tile.xCoord, tile.yCoord, tile.zCoord);
        boolean open = BlockTrapDoor.func_150118_d(metadata);
        height = TransTrapdoor.isTrapdoorSetLow(metadata) ? (open ? 0.2 : 0.15) : (open ? 0.95 : 0.85);
        renderPortal(tile, x, y, z, 0.5, 0.5, riftPath, 1.0F);
    }
}
