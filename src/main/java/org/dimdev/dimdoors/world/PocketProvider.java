package org.dimdev.dimdoors.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.client.IRenderHandler;

import org.dimdev.dimdoors.CloudRenderBlank;
import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.core.DimensionType;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.pockets.Pocket;
import org.dimdev.dimdoors.pockets.SharedPockets;
import org.dimdev.dimdoors.ticking.CustomLimboPopulator;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PocketProvider extends WorldProvider {

    private final DDProperties properties;
    protected CustomLimboPopulator spawner;
    protected IRenderHandler skyRenderer;

    public PocketProvider() {
        this.hasNoSky = true;
        this.skyRenderer = new PocketSkyProvider();

        this.spawner = DimDoors.spawner;
        this.properties = DimDoors.properties;
    }

    @Override
    protected void registerWorldChunkManager() {
        super.worldChunkMgr = new WorldChunkManagerHell(DimDoors.pocketBiome, 1);
    }

    /** The pocket type this shared dimension holds, or null if it is used for anything else. */
    protected DimensionType sharedType() {
        return SharedPockets.typeOf(dimensionId);
    }

    @Override
    public String getSaveFolder() {
        DimensionType shared = sharedType();
        return shared != null ? "DimensionalDoors/" + SharedPockets.folderOf(shared) : null;
    }

    @Override
    public Vec3 getSkyColor(Entity cameraEntity, float partialTicks) {
        setCloudRenderer(new CloudRenderBlank());
        return Vec3.createVectorHelper(0d, 0d, 0d);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Vec3 getFogColor(float par1, float par2) {
        return Vec3.createVectorHelper(0d, 0d, 0d);
    }

    @Override
    public double getHorizon() {
        return worldObj.getHeight();
    }

    @Override
    public IChunkProvider createChunkGenerator() {
        return new PocketGenerator(worldObj, dimensionId, false, spawner);
    }

    @Override
    public boolean canSnowAt(int x, int y, int z, boolean light) {
        return false;
    }

    @Override
    public boolean canBlockFreeze(int x, int y, int z, boolean byWater) {
        return false;
    }

    public float calculateCelestialAngle(long par1, float par3) {
        return .5F;
    }

    @Override
    protected void generateLightBrightnessTable() {
        DimensionType type = sharedType();
        if (type == null || type == DimensionType.POCKET) {
            super.generateLightBrightnessTable();
            return;
        }

        for (int steps = 0; steps <= 15; ++steps) {
            float var3 = (float) (Math.pow(steps, 1.5) / Math.pow(15.0F, 1.5));
            this.lightBrightnessTable[15 - steps] = var3;
        }
    }

    @Override
    public String getDimensionName() {
        DimensionType shared = sharedType();
        return shared != null ? SharedPockets.nameOf(shared) : "PocketDim " + this.dimensionId;
    }

    @Override
    public int getRespawnDimension(EntityPlayerMP player) {
        int respawnDim;

        if (properties.LimboEnabled) {
            respawnDim = properties.LimboDimensionID;
        } else {
            // The real dimension the pocket the player is in hangs from
            Pocket pocket = SharedPockets
                .pocketAt(dimensionId, MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posZ));
            respawnDim = pocket != null ? pocket.rootDimension() : 0;
        }
        // TODO: Are we sure we need to load the dimension as well? Why can't the game handle that?
        PocketManager.loadDimension(respawnDim);
        return respawnDim;
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }

    @Override
    public int getActualHeight() {
        return 256;
    }
}
