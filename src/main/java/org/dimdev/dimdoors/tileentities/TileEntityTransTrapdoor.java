package org.dimdev.dimdoors.tileentities;

import java.util.Random;

import net.minecraft.util.AxisAlignedBB;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.client.ClosingRifts;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityTransTrapdoor extends DDTileEntityBase {

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public float[] getRenderColor(Random rand) {
        float[] rgbaColor = { 1, 1, 1, 1 };
        if (this.worldObj.provider.dimensionId == DimDoors.NETHER_DIMENSION_ID) {
            rgbaColor[0] = worldObj.rand.nextFloat() * 0.5F + 0.4F;
            rgbaColor[1] = worldObj.rand.nextFloat() * 0.05F;
            rgbaColor[2] = worldObj.rand.nextFloat() * 0.05F;
        } else {
            rgbaColor[0] = worldObj.rand.nextFloat() * 0.5F + 0.1F;
            rgbaColor[1] = worldObj.rand.nextFloat() * 0.4F + 0.4F;
            rgbaColor[2] = worldObj.rand.nextFloat() * 0.6F + 0.5F;
        }
        return rgbaColor;
    }

    /** Grown while a rift's crack closes into the trapdoor. */
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return ClosingRifts.expand(this, super.getRenderBoundingBox());
    }

    /** Drawn with the world and again after water, for the portal's rim (PortalRenderer). */
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }
}
