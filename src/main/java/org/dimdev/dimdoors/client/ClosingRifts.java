package org.dimdev.dimdoors.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.blocks.IDimDoor;
import org.dimdev.dimdoors.tileentities.RiftClosing;
import org.dimdev.dimdoors.tileentities.TileEntityRift;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Rifts closing on this client. A rift seen to start closing gets its sound and its crack's clock here, and the crack
 * of a rift a door was set on is kept for the door's renderer to draw closing into it, with the same sound.
 */
@SideOnly(Side.CLIENT)
public final class ClosingRifts {

    /** A close seen this many ticks after it started is shown part way through, and silent. */
    private static final int LATE_TICKS = 10;

    private static final Map<ChunkCoordinates, Ghost> GHOSTS = new HashMap<>();
    private static World ghostWorld;

    private ClosingRifts() {}

    /** The crack of a rift a door took the place of. */
    private static final class Ghost {

        final RiftClosing closing;
        final long startMillis;
        final int curveId;
        final float rotation;
        final double size;

        Ghost(RiftClosing closing, long startMillis, TileEntityRift rift) {
            this.closing = closing;
            this.startMillis = startMillis;
            this.curveId = rift.curveId;
            this.rotation = rift.riftRotation;
            this.size = rift.crackSize();
        }

        double seconds() {
            return (Minecraft.getSystemTime() - startMillis) / 1000D;
        }
    }

    static void started(TileEntityRift rift) {
        long late = rift.getWorldObj()
            .getTotalWorldTime() - rift.closingSince;
        long now = Minecraft.getSystemTime();
        if (late <= LATE_TICKS) {
            rift.closeStartMillis = now;
            play(rift.closing(), rift);
        } else rift.closeStartMillis = now - late * 50;
    }

    static void replaced(TileEntityRift rift) {
        World world = rift.getWorldObj();
        if (!(world.getBlock(rift.xCoord, rift.yCoord, rift.zCoord) instanceof IDimDoor)) return;
        RiftClosing closing;
        long start;
        if (rift.shouldClose && rift.closeStartMillis >= 0) {
            // Already closing: the crack carries on into the door
            closing = rift.closing();
            start = rift.closeStartMillis;
        } else {
            closing = RiftClosing.of(rift.radius() >= TileEntityRift.BIG_RADIUS, rift.curveId);
            start = Minecraft.getSystemTime();
            play(closing, rift);
        }
        if (world != ghostWorld) {
            GHOSTS.clear();
            ghostWorld = world;
        }
        for (Iterator<Ghost> it = GHOSTS.values()
            .iterator(); it.hasNext();) {
            Ghost ghost = it.next();
            if (ghost.seconds() >= ghost.closing.end()) it.remove();
        }
        GHOSTS.put(new ChunkCoordinates(rift.xCoord, rift.yCoord, rift.zCoord), new Ghost(closing, start, rift));
    }

    private static void play(RiftClosing closing, TileEntityRift rift) {
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(
                new PositionedSoundRecord(
                    new ResourceLocation(closing.sound),
                    closing.isBig() ? 1.5F : 1,
                    1,
                    rift.xCoord + .5F,
                    rift.yCoord + .5F,
                    rift.zCoord + .5F));
    }

    private static Ghost ghost(TileEntity door) {
        if (GHOSTS.isEmpty() || door.getWorldObj() != ghostWorld) return null;
        return GHOSTS.get(new ChunkCoordinates(door.xCoord, door.yCoord, door.zCoord));
    }

    /** Draws the crack closing into a door, if one is; for the door renderers. */
    static void draw(TileEntity door, double x, double y, double z) {
        if (MinecraftForgeClient.getRenderPass() != 0) return;
        Ghost ghost = ghost(door);
        if (ghost == null) return;
        double seconds = ghost.seconds();
        if (seconds >= ghost.closing.end()) {
            GHOSTS.remove(new ChunkCoordinates(door.xCoord, door.yCoord, door.zCoord));
            return;
        }
        RenderRift.drawCrack(
            ghost.rotation,
            ghost.curveId,
            ghost.size * ghost.closing.scale(seconds),
            ghost.closing.twist(seconds),
            x + .5,
            y + .5,
            z + .5);
    }

    /** A door's render box, grown to hold a crack closing into it. */
    public static AxisAlignedBB expand(TileEntity door, AxisAlignedBB box) {
        Ghost ghost = ghost(door);
        if (ghost == null) return box;
        double crack = ghost.size * (1 + RiftClosing.SWELL);
        double reach = 1 + crack / 2 + DimDoors.properties.RiftJitter * TileEntityRift.CRACK_SHAKE * crack;
        return box.func_111270_a(
            AxisAlignedBB.getBoundingBox(
                door.xCoord + .5 - reach,
                door.yCoord + .5 - reach,
                door.zCoord + .5 - reach,
                door.xCoord + .5 + reach,
                door.yCoord + .5 + reach,
                door.zCoord + .5 + reach));
    }
}
