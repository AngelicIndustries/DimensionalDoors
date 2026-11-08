package org.dimdev.dimdoors.tileentities;

import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.AxisAlignedBB;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.watcher.ClientLinkData;

public class TileEntityDimDoor extends DDTileEntityBase {

    public boolean openOrClosed;
    public int orientation;
    public boolean hasExit;
    public byte lockStatus;
    public boolean isDungeonChainLink;
    public boolean hasGennedPair = false;

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        if (PocketManager.getLink(xCoord, yCoord, zCoord, worldObj) != null) {
            ClientLinkData linkData = new ClientLinkData(PocketManager.getLink(xCoord, yCoord, zCoord, worldObj));
            NBTTagCompound link = new NBTTagCompound();
            linkData.writeToNBT(link);
            tag.setTag("Link", link);
        }
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        NBTTagCompound tag = pkt.func_148857_g();
        readFromNBT(tag);

        if (tag.hasKey("Link")) {
            ClientLinkData linkData = ClientLinkData.readFromNBT(tag.getCompoundTag("Link"));
            PocketManager.getLinkWatcher()
                .onCreated(linkData);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        try {
            this.openOrClosed = nbt.getBoolean("openOrClosed");
            this.orientation = nbt.getInteger("orientation");
            this.hasExit = nbt.getBoolean("hasExit");
            this.isDungeonChainLink = nbt.getBoolean("isDungeonChainLink");
            this.hasGennedPair = nbt.getBoolean("hasGennedPair");
        } catch (Exception e) // ???
        {

        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setBoolean("openOrClosed", this.openOrClosed);
        nbt.setBoolean("hasExit", this.hasExit);
        nbt.setInteger("orientation", this.orientation);
        nbt.setBoolean("isDungeonChainLink", isDungeonChainLink);
        nbt.setBoolean("hasGennedPair", hasGennedPair);
    }

    /** The doorway is drawn from the upper block down through the lower one, so both have to be in view. */
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return AxisAlignedBB.getBoundingBox(xCoord, yCoord - 1, zCoord, xCoord + 1, yCoord + 1, zCoord + 1);
    }

    @Override
    public float[] getRenderColor(Random rand) {
        float[] rgbaColor = { 1, 1, 1, 1 };
        if (this.worldObj.provider.dimensionId == DimDoors.NETHER_DIMENSION_ID) {
            rgbaColor[0] = rand.nextFloat() * 0.5F + 0.4F;
            rgbaColor[1] = rand.nextFloat() * 0.05F;
            rgbaColor[2] = rand.nextFloat() * 0.05F;
        } else {
            rgbaColor[0] = rand.nextFloat() * 0.5F + 0.1F;
            rgbaColor[1] = rand.nextFloat() * 0.4F + 0.4F;
            rgbaColor[2] = rand.nextFloat() * 0.6F + 0.5F;
        }
        return rgbaColor;
    }

    /** Drawn with the world and again after water, for the portal's rim (PortalRenderer). */
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }
}
