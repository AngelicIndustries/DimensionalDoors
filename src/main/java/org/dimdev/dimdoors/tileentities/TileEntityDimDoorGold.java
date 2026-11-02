package org.dimdev.dimdoors.tileentities;

import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import org.dimdev.dimdoors.IChunkLoader;
import org.dimdev.dimdoors.core.NewDimData;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.helpers.ChunkLoaderHelper;
import org.dimdev.dimdoors.pockets.Pocket;
import org.dimdev.dimdoors.pockets.SharedPockets;
import org.dimdev.dimdoors.world.PocketBuilder;

public class TileEntityDimDoorGold extends TileEntityDimDoor implements IChunkLoader {

    private Ticket chunkTicket;
    private boolean initialized = false;

    @Override
    public boolean canUpdate() {
        return !initialized;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void updateEntity() {
        if (!initialized) {
            initialize(null);
        }
    }

    @Override
    public void initialize(Ticket ticket) {
        initialized = true;
        chunkTicket = ticket;

        // Only do anything if this function is running on the server side
        // NOTE: We don't have to check whether this block is the upper door
        // block or the lower one because only one of them should have a
        // link associated with it.
        if (!worldObj.isRemote) {
            NewDimData dimension = PocketManager.createDimensionData(worldObj);
            Pocket pocket = SharedPockets.pocketAt(worldObj.provider.dimensionId, xCoord, zCoord);

            // Check whether a ticket has already been assigned to this door
            if (chunkTicket == null) {
                // No ticket yet.
                // Check if this area should be loaded and request a new ticket.
                if (isValidChunkLoaderSetup(dimension, pocket)) {
                    chunkTicket = ChunkLoaderHelper.createTicket(xCoord, yCoord, zCoord, worldObj);
                }
            } else {
                // A ticket has already been provided.
                // Check if this area should be loaded. If not, release the ticket.
                if (!isValidChunkLoaderSetup(dimension, pocket)) {
                    ForgeChunkManager.releaseTicket(chunkTicket);
                    chunkTicket = null;
                }
            }

            // If chunkTicket isn't null at this point, then this is a valid door setup.
            // The last step is to request force loading of the pocket's chunks.
            if (chunkTicket != null) {
                ChunkLoaderHelper.forcePocketChunks(pocket, chunkTicket);
            }
        }
    }

    private boolean isValidChunkLoaderSetup(NewDimData dimension, Pocket pocket) {
        // Check the various conditions that make this a valid door setup.
        // 1. The door must be inside the pocket's XZ boundaries,
        // to prevent loading of chunks with a distant door
        // 2. The door must be in a built pocket
        // 3. The door must be linked so that it's clear that it's not a normal door

        return (pocket != null && pocket.origin() != null
            && dimension.getLink(xCoord, yCoord, zCoord) != null
            && PocketBuilder.calculateDefaultBounds(pocket)
                .contains(xCoord, yCoord, zCoord));
    }

    @Override
    public void invalidate() {
        ForgeChunkManager.releaseTicket(chunkTicket);
        super.invalidate();
    }
}
