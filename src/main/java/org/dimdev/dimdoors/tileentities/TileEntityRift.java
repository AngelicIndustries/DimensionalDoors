package org.dimdev.dimdoors.tileentities;

import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.core.DimLink;
import org.dimdev.dimdoors.core.NewDimData;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.util.Point4D;
import org.dimdev.dimdoors.watcher.ClientLinkData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityRift extends DDTileEntityBase {

    private static final int RIFT_INTERACTION_RANGE = 5;
    private static final int MAX_ANCESTOR_LINKS = 2;
    private static final int MAX_CHILD_LINKS = 1;
    private static final int ENDERMAN_SPAWNING_CHANCE = 1;
    private static final int MAX_ENDERMAN_SPAWNING_CHANCE = 32;
    private static final int RIFT_SPREAD_CHANCE = 1;
    private static final int MAX_RIFT_SPREAD_CHANCE = 256;
    private static final int HOSTILE_ENDERMAN_CHANCE = 1;
    private static final int MAX_HOSTILE_ENDERMAN_CHANCE = 3;
    private static final int UPDATE_PERIOD = 200;
    private static final int CLOSING_PERIOD = 40;
    // Growth follows the 1.12 rewrite: the size grows like the square root of the time the rift has been loaded, and
    // it unravels a block within size / 400 blocks with a chance of size / 500000 each tick (see unravel for where)
    private static final float SIZE_PER_BLOCK = 400;
    private static final float MAX_UNRAVEL_CHANCE = 500000;
    // Single rifts spawn Endermen once they unravel this far (about an hour loaded at the default speed)
    private static final float ENDERMAN_RADIUS = 3;
    /**
     * How far a crack shakes, as a share of its width. 1.12 used the cube of the width, up to 100 blocks on old rifts.
     */
    public static final double CRACK_SHAKE = 0.03;

    private static final Random random = new Random();

    private int updateTimer;
    private final int closeTimer = 0;
    public int xOffset = 0;
    public int yOffset = 0;
    public int zOffset = 0;
    public boolean shouldClose = false;
    public Point4D nearestRiftLocation = null;
    public int spawnedEndermenID = 0;

    public int riftRotation = random.nextInt(360);
    public float size = 0;
    /** Which crack shape this rift draws; the client takes it modulo the number of shapes. */
    public int curveId = random.nextInt(1 << 16);
    /** The core's spin, client only. */
    public float renderAngle;
    private boolean stabilized = false;
    private int syncedRadius;

    public TileEntityRift() {
        // Vary the update times of rifts to prevent all the rifts in a cluster
        // from updating at the same time.
        updateTimer = random.nextInt(UPDATE_PERIOD);

    }

    @Override
    public void updateEntity() {
        if (PocketManager.getLink(xCoord, yCoord, zCoord, worldObj.provider.dimensionId) == null) {
            if (worldObj.getBlock(xCoord, yCoord, zCoord) == DimDoors.blockRift) {
                worldObj.setBlockToAir(xCoord, yCoord, zCoord);
            } else {
                invalidate();
            }
            return;
        }

        if (worldObj.getBlock(xCoord, yCoord, zCoord) != DimDoors.blockRift) {
            invalidate();
            return;
        }

        // Check if this rift should render white closing particles and
        // spread the closing effect to other rifts nearby.
        if (shouldClose) {
            closeRift();
            return;
        }

        DDProperties properties = DimDoors.properties;
        if (!stabilized) {
            grow(properties);
            if (worldObj.isRemote) {
                spawnGrowthParticles();
            } else if (properties.RiftGriefingEnabled && random.nextFloat() * MAX_UNRAVEL_CHANCE < size) {
                unravel();
            }
        }

        if (updateTimer >= UPDATE_PERIOD) {
            spawnEndermen(properties);
            syncSize();
            updateTimer = 0;
        } else if (updateTimer == UPDATE_PERIOD / 2) {
            updateNearestRift();
            spread(properties);
        }
        updateTimer++;
    }

    /**
     * How wide this rift's crack is drawn, in blocks: a block past the hollow it eats. 1.12 drew it a third bigger than
     * that, which got huge on old rifts.
     */
    public double crackSize() {
        return DimDoors.properties.RiftSize * (2 * radius() + 2);
    }

    /** How far around it this rift unravels blocks. */
    public float radius() {
        return size / SIZE_PER_BLOCK;
    }

    public boolean isStabilized() {
        return stabilized;
    }

    /** Stops this rift growing, unravelling blocks and spawning Endermen, for good (the Rift Stabilizer). */
    public void stabilize() {
        stabilized = true;
        markDirty();
        if (!worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    private void grow(DDProperties properties) {
        if (properties.RiftGrowthSpeed <= 0 || properties.MaxRiftRadius > 0 && radius() >= properties.MaxRiftRadius) {
            return;
        }
        for (int n = 0; n < 10; n++) {
            size += properties.RiftGrowthSpeed / (size + 1);
        }
    }

    /**
     * Unravels the nearest block in a random direction within the radius (at least the blocks next to it), so a rift
     * eats out a hollow around itself that widens as it grows. Spread evenly through the radius instead, as in the 1.12
     * rewrite, most attempts land in air and the damage thins out as the rift grows. A block the rift can't unravel,
     * such as a liquid or an immune block, shields what lies behind it.
     */
    private void unravel() {
        double dy = random.nextDouble() * 2 - 1;
        double angle = random.nextDouble() * Math.PI * 2;
        double flat = Math.sqrt(1 - dy * dy);
        double dx = flat * Math.cos(angle), dz = flat * Math.sin(angle);
        float reach = Math.max(1.5F, radius());
        for (double d = 1; d <= reach; d += 0.5) {
            int x = MathHelper.floor_double(xCoord + 0.5 + dx * d);
            int y = MathHelper.floor_double(yCoord + 0.5 + dy * d);
            int z = MathHelper.floor_double(zCoord + 0.5 + dz * d);
            if (x == xCoord && y == yCoord && z == zCoord || worldObj.isAirBlock(x, y, z)) continue;
            DimDoors.blockRift.unravel(worldObj, x, y, z, random);
            return;
        }
    }

    /** Clients grow their copy too; this corrects it each time the radius passes another block. */
    private void syncSize() {
        int radius = (int) radius();
        if (!worldObj.isRemote && radius != syncedRadius) {
            syncedRadius = radius;
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    /** A growing rift's cloud of particles gets denser and wider with its size. */
    private void spawnGrowthParticles() {
        float radius = radius();
        if (random.nextFloat() >= Math.min(1, radius / 4)) {
            return;
        }
        double spread = 0.15 + 0.08 * Math.min(radius, 10);
        DimDoors.proxy.spawnRiftParticle(
            worldObj,
            xCoord + 0.5 + random.nextGaussian() * spread,
            yCoord + 0.5 + random.nextGaussian() * spread,
            zCoord + 0.5 + random.nextGaussian() * spread,
            random.nextGaussian() * 0.01,
            random.nextGaussian() * 0.01,
            random.nextGaussian() * 0.01);
    }

    private void spawnEndermen(DDProperties properties) {
        if (worldObj.isRemote || !properties.RiftsSpawnEndermenEnabled || stabilized) {
            return;
        }

        // Ensure that this rift is only spawning one Enderman at a time, to prevent hordes of Endermen
        Entity entity = worldObj.getEntityByID(this.spawnedEndermenID);
        if (entity != null && entity instanceof EntityEnderman) {
            return;
        }

        if (random.nextInt(MAX_ENDERMAN_SPAWNING_CHANCE) < ENDERMAN_SPAWNING_CHANCE) {
            // Endermen will only spawn from groups of rifts and from rifts that have grown a while
            if (updateNearestRift() || radius() >= ENDERMAN_RADIUS) {
                List<EntityEnderman> list = worldObj.getEntitiesWithinAABB(
                    EntityEnderman.class,
                    AxisAlignedBB
                        .getBoundingBox(xCoord - 9, yCoord - 3, zCoord - 9, xCoord + 9, yCoord + 3, zCoord + 9));

                if (list.isEmpty()) {
                    EntityEnderman enderman = new EntityEnderman(worldObj);
                    enderman.setLocationAndAngles(xCoord + 0.5, yCoord - 1, zCoord + 0.5, 5, 6);
                    worldObj.spawnEntityInWorld(enderman);
                    spawnedEndermenID = enderman.getEntityId();

                    if (random.nextInt(MAX_HOSTILE_ENDERMAN_CHANCE) < HOSTILE_ENDERMAN_CHANCE) {
                        EntityPlayer player = this.worldObj.getClosestPlayerToEntity(enderman, 50);
                        if (player != null) {
                            enderman.setTarget(player);
                        }
                    }
                }
            }
        }
    }

    private void closeRift() {
        NewDimData dimension = PocketManager.createDimensionData(worldObj);
        if (size < CLOSING_PERIOD / 2) {
            for (DimLink riftLink : dimension.findRiftsInRange(worldObj, 6, xCoord, yCoord, zCoord)) {
                Point4D location = riftLink.source();
                TileEntity entity = worldObj.getTileEntity(location.getX(), location.getY(), location.getZ());
                if (!(entity instanceof TileEntityRift)) continue;
                TileEntityRift rift = (TileEntityRift) entity;
                if (!rift.shouldClose) {
                    rift.shouldClose = true;
                    rift.markDirty();
                    if (!worldObj.isRemote) {
                        worldObj.markBlockForUpdate(location.getX(), location.getY(), location.getZ());
                    }
                }
            }
        }
        if (size <= 0 && !worldObj.isRemote) {
            DimLink link = PocketManager.getLink(this.xCoord, this.yCoord, this.zCoord, worldObj);
            if (link != null && !worldObj.isRemote) {
                dimension.deleteLink(link);
            }
            worldObj.setBlockToAir(xCoord, yCoord, zCoord);
            worldObj.playSound(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "mods.DimDoors.sfx.riftClose", 0.7f, 1, false);
        }

        // Grown rifts close about as fast as new ones
        size -= Math.max(1, size / CLOSING_PERIOD);
    }

    public boolean updateNearestRift() {
        Point4D previousNearest = nearestRiftLocation;
        DimLink nearestRiftLink = PocketManager.createDimensionData(worldObj)
            .findNearestRift(worldObj, RIFT_INTERACTION_RANGE, xCoord, yCoord, zCoord);

        nearestRiftLocation = (nearestRiftLink == null) ? null : nearestRiftLink.source();

        // If the nearest rift location changed, then update particle offsets
        if (previousNearest != nearestRiftLocation && (previousNearest == null || nearestRiftLocation == null
            || !previousNearest.equals(nearestRiftLocation))) {
            updateParticleOffsets();
        }
        return (nearestRiftLocation != null);
    }

    private void updateParticleOffsets() {
        if (nearestRiftLocation != null) {
            this.xOffset = this.xCoord - nearestRiftLocation.getX();
            this.yOffset = this.yCoord - nearestRiftLocation.getY();
            this.zOffset = this.zCoord - nearestRiftLocation.getZ();
        } else {
            this.xOffset = 0;
            this.yOffset = 0;
            this.xOffset = 0;
        }
        this.markDirty();
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    public int countAncestorLinks(DimLink link) {
        if (link.parent() != null) {
            return countAncestorLinks(link.parent()) + 1;
        }
        return 0;
    }

    public void spread(DDProperties properties) {
        if (worldObj.isRemote || !properties.RiftSpreadEnabled
            || random.nextInt(MAX_RIFT_SPREAD_CHANCE) < RIFT_SPREAD_CHANCE
            || this.shouldClose
            || this.stabilized) {
            return;
        }

        NewDimData dimension = PocketManager.createDimensionData(worldObj);
        DimLink link = dimension.getLink(xCoord, yCoord, zCoord);

        if (link.childCount() >= MAX_CHILD_LINKS || countAncestorLinks(link) >= MAX_ANCESTOR_LINKS) {
            return;
        }

        // The probability of rifts trying to spread increases if more rifts are nearby.
        // Players should see rifts spread faster within clusters than at the edges of clusters.
        // Also, single rifts CANNOT spread.
        int nearRifts = dimension.findRiftsInRange(worldObj, RIFT_INTERACTION_RANGE, xCoord, yCoord, zCoord)
            .size();
        if (nearRifts == 0 || random.nextInt(nearRifts) == 0) {
            return;
        }
        DimDoors.blockRift.spreadRift(dimension, link, worldObj, random);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.updateTimer = nbt.getInteger("updateTimer");
        this.xOffset = nbt.getInteger("xOffset");
        this.yOffset = nbt.getInteger("yOffset");
        this.zOffset = nbt.getInteger("zOffset");
        this.shouldClose = nbt.getBoolean("shouldClose");
        this.spawnedEndermenID = nbt.getInteger("spawnedEndermenID");
        this.riftRotation = nbt.getInteger("riftRotation");
        this.size = nbt.getFloat("size");
        // Rifts saved before cracks were drawn pick a shape from where they are
        this.curveId = nbt.hasKey("curveId") ? nbt.getInteger("curveId") : xCoord * 31 + yCoord * 17 + zCoord;
        this.stabilized = nbt.getBoolean("stabilized");

    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("updateTimer", this.updateTimer);
        nbt.setInteger("xOffset", this.xOffset);
        nbt.setInteger("yOffset", this.yOffset);
        nbt.setInteger("zOffset", this.zOffset);
        nbt.setBoolean("shouldClose", this.shouldClose);
        nbt.setInteger("spawnedEndermenID", this.spawnedEndermenID);
        nbt.setInteger("riftRotation", this.riftRotation);
        nbt.setFloat("size", this.size);
        nbt.setInteger("curveId", this.curveId);
        nbt.setBoolean("stabilized", this.stabilized);

    }

    /** Big cracks reach far past the rift block, so they mustn't be culled with it. */
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        double crack = crackSize();
        double reach = 1 + crack / 2 + DimDoors.properties.RiftJitter * CRACK_SHAKE * crack;
        return AxisAlignedBB.getBoundingBox(
            xCoord + 0.5 - reach,
            yCoord + 0.5 - reach,
            zCoord + 0.5 - reach,
            xCoord + 0.5 + reach,
            yCoord + 0.5 + reach,
            zCoord + 0.5 + reach);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        double distance = 64 + crackSize() / 2;
        return distance * distance;
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
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 0, tag);
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
    public float[] getRenderColor(Random rand) {
        return null;
    }
}
