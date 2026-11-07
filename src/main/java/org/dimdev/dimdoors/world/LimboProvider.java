package org.dimdev.dimdoors.world;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.client.IRenderHandler;

import org.dimdev.dimdoors.CloudRenderBlank;
import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.client.LimboFog;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.pockets.SharedPockets;
import org.dimdev.dimdoors.ticking.CustomLimboPopulator;
import org.dimdev.dimdoors.util.Point4D;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class LimboProvider extends WorldProvider {

    @SideOnly(Side.CLIENT)
    private static IRenderHandler noClouds;

    @Override
    public String getDimensionName() {
        return "Limbo";
    }

    private final IRenderHandler skyRenderer;
    private final DDProperties properties;
    private final CustomLimboPopulator spawner;

    public LimboProvider() {
        this.hasNoSky = false;
        this.skyRenderer = new LimboSkyProvider();
        this.spawner = DimDoors.spawner;
        this.properties = DimDoors.properties;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IRenderHandler getSkyRenderer() {
        return this.skyRenderer;
    }

    /** No clouds over Limbo. Set here rather than as the sky is drawn, which some renderers never ask for. */
    @Override
    @SideOnly(Side.CLIENT)
    public IRenderHandler getCloudRenderer() {
        if (noClouds == null) noClouds = new CloudRenderBlank();
        return noClouds;
    }

    @Override
    protected void registerWorldChunkManager() {
        super.worldChunkMgr = new WorldChunkManagerHell(DimDoors.limboBiome, 1);
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return DimDoors.limboBiome;
    }

    @Override
    public boolean canRespawnHere() {
        return properties.HardcoreLimboEnabled;
    }

    @Override
    public boolean isBlockHighHumidity(int x, int y, int z) {
        return false;
    }

    @Override
    public boolean canSnowAt(int x, int y, int z, boolean checkLight) {
        return false;
    }

    /**
     * No weather in Limbo. It shares the Overworld's, and though no rain falls here, a shader pack seeing rain draws
     * its rain sky and fog over Limbo's dark (Complementary does).
     */
    @Override
    public void updateWeather() {
        worldObj.prevRainingStrength = worldObj.rainingStrength = 0;
        worldObj.prevThunderingStrength = worldObj.thunderingStrength = 0;
    }

    @Override
    protected void generateLightBrightnessTable() {
        if (properties.DarkLimbo) {
            // Vanilla's curve: unlit places are black, unless Limbo Ambient Light lifts them
            // (MixinEntityRendererLimboLight)
            for (int level = 0; level <= 15; ++level) {
                float dark = 1.0F - level / 15.0F;
                this.lightBrightnessTable[level] = (1.0F - dark) / (dark * 3.0F + 1.0F);
            }
            return;
        }
        float modifier = 0.0F;

        for (int steps = 0; steps <= 15; ++steps) {
            float var3 = 1.0F - steps / 15.0F;
            this.lightBrightnessTable[steps] = ((0.0F + var3) / (var3 * 3.0F + 1.0F) * (1.0F - modifier) + modifier)
                * 3;
            // System.out.println( this.lightBrightnessTable[steps]+"light");
        }
    }

    @Override
    public ChunkCoordinates getSpawnPoint() {

        return this.getRandomizedSpawnPoint();
    }

    @Override
    public float calculateCelestialAngle(long par1, float par3) {
        int var4 = (int) (par1 % 24000L);
        float var5 = (var4 + par3) / 24000.0F - 0.25F;

        if (var5 < 0.0F) {
            ++var5;
        }

        if (var5 > 1.0F) {
            --var5;
        }

        float var6 = var5;
        var5 = 1.0F - (float) ((Math.cos(var5 * Math.PI) + 1.0D) / 2.0D);
        var5 = var6 + (var5 - var6) / 3.0F;
        // Dark Limbo is a moonless midnight (Limbo's own moon overhead, no stars); otherwise a noon that never ends
        return properties.DarkLimbo ? 0.5F : 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public float getStarBrightness(float partialTicks) {
        return properties.DarkLimbo ? 0 : super.getStarBrightness(partialTicks);
    }

    @SideOnly(Side.CLIENT)
    public int getMoonPhase(long par1, float par3) {
        return 4;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public String getSaveFolder() {
        return (dimensionId == 0 ? null : "DimensionalDoors/Limbo" + dimensionId);
    }

    @Override
    public boolean canCoordinateBeSpawn(int par1, int par2) {
        Block block = this.worldObj.getTopBlock(par1, par2);
        return block == DimDoors.blockLimbo;
    }

    @Override
    public double getHorizon() {
        return worldObj.getHeight() / 4 - 800;
    }

    @Override
    public Vec3 getSkyColor(Entity cameraEntity, float partialTicks) {
        return Vec3.createVectorHelper(0, 0, 0);

    }

    @SideOnly(Side.CLIENT)
    @Override
    public Vec3 getFogColor(float par1, float par2) {
        // Black under the thick fog (LimboFog), the old grey without it
        double grey = LimboFog.density() > 0 ? 0 : .2;
        return Vec3.createVectorHelper(grey, grey, grey);

    }

    @Override
    public int getRespawnDimension(EntityPlayerMP player) {
        return 0;
    }

    @Override
    public IChunkProvider createChunkGenerator() {
        // TODO: ...We're passing the LimboGenerator a fixed seed. We should be passing the world seed! @_@ ~SenseiKiwi
        return new LimboGenerator(worldObj, 45, spawner, properties);
    }

    @Override
    public boolean canBlockFreeze(int x, int y, int z, boolean byWater) {
        return false;
    }

    public static Point4D getLimboSkySpawn(EntityPlayer player, DDProperties properties) {
        // From a pocket, where its virtual location puts the player in the real world
        Point4D from = SharedPockets.toVirtual(
            new Point4D((int) player.posX, (int) player.posY, (int) player.posZ, player.worldObj.provider.dimensionId));
        int x = from.getX() + MathHelper
            .getRandomIntegerInRange(player.worldObj.rand, -properties.LimboEntryRange, properties.LimboEntryRange);
        int z = from.getZ() + MathHelper
            .getRandomIntegerInRange(player.worldObj.rand, -properties.LimboEntryRange, properties.LimboEntryRange);
        return new Point4D(x, 700, z, properties.LimboDimensionID);
    }

    @Override
    public ChunkCoordinates getRandomizedSpawnPoint() {
        int x = MathHelper.getRandomIntegerInRange(this.worldObj.rand, -500, 500);
        int z = MathHelper.getRandomIntegerInRange(this.worldObj.rand, -500, 500);
        return new ChunkCoordinates(x, 700, z);
    }
}
