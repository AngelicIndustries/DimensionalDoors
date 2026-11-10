package org.dimdev.dimdoors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import org.dimdev.dimdoors.CommonProxy;
import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.blocks.BaseDimDoor;
import org.dimdev.dimdoors.blocks.BlockFadebloom;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.core.DimLink;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.ticking.EntityFallingDripstone;
import org.dimdev.dimdoors.ticking.MobMonolith;
import org.dimdev.dimdoors.tileentities.TileEntityDimDoor;
import org.dimdev.dimdoors.tileentities.TileEntityRift;
import org.dimdev.dimdoors.tileentities.TileEntityTransTrapdoor;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class ClientProxy extends CommonProxy {

    @Override
    public void registerRenderers() {
        // MinecraftForgeClient.preloadTexture(BLOCK_PNG);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityDimDoor.class, new RenderDimDoor());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTransTrapdoor.class, new RenderTransTrapdoor());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityRift.class, new RenderRift());
        // Build the crack shapes now rather than on the first rift in sight
        RiftCurves.get(0);

        // MinecraftForgeClient.preloadTexture(RIFT2_PNG);
        RenderingRegistry.registerEntityRenderingHandler(MobMonolith.class, RenderMobObelisk.create(.5F));
        RenderingRegistry.registerEntityRenderingHandler(EntityFallingDripstone.class, new RenderFallingDripstone());
        RenderingRegistry.registerBlockHandler(new PrivatePocketRender(RenderingRegistry.getNextAvailableRenderId()));
        if (DimDoors.fadebloom != null) {
            BlockFadebloom.renderType = RenderingRegistry.getNextAvailableRenderId();
            RenderingRegistry.registerBlockHandler(new FadebloomRender(BlockFadebloom.renderType));
        }
        FMLCommonHandler.instance()
            .bus()
            .register(new MonolithSounds());
        FMLCommonHandler.instance()
            .bus()
            .register(new LimboSounds());
        FMLCommonHandler.instance()
            .bus()
            .register(new PortalSounds());
        FMLCommonHandler.instance()
            .bus()
            .register(new RiftSounds());
        FMLCommonHandler.instance()
            .bus()
            .register(new StaticSounds());
        FMLCommonHandler.instance()
            .bus()
            .register(new TrepidationHeartbeat());

    }

    @Override
    public void spawnMonolithParticle(World world, double x, double y, double z, double motionX, double motionY,
        double motionZ) {
        Minecraft mc = Minecraft.getMinecraft();
        // As vanilla's particle settings would treat a portal particle
        int setting = mc.gameSettings.particleSetting;
        if (setting == 2 || setting == 1 && world.rand.nextInt(3) == 0) return;
        mc.effectRenderer.addEffect(new MonolithPortalFX(world, x, y, z, motionX, motionY, motionZ));
    }

    @Override
    public void spawnRiftParticle(World world, double x, double y, double z, double motionX, double motionY,
        double motionZ) {
        Minecraft mc = Minecraft.getMinecraft();
        int setting = mc.gameSettings.particleSetting;
        if (setting == 2 || setting == 1 && world.rand.nextInt(3) == 0) return;
        mc.effectRenderer.addEffect(new GoggleRiftFX(world, x, y, z, motionX, motionY, motionZ, mc.effectRenderer));
    }

    @Override
    public void updateDoorTE(BaseDimDoor door, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityDimDoor) {
            DimLink link = PocketManager.getLink(x, y, z, world);
            int metadata = world.getBlockMetadata(x, y, z);
            TileEntityDimDoor dimTile = (TileEntityDimDoor) tile;
            dimTile.openOrClosed = door.isDoorOnRift(world, x, y, z) && door.isUpperDoorBlock(metadata);
            dimTile.orientation = door.func_150012_g(world, x, y, z) & 7;
            dimTile.lockStatus = door.getLockStatus(world, x, y, z);
        }
    }

    @Override
    public void printStringClient(String string) {}

    @Override
    public void highlightRiftCores() {
        RenderRift.showCoresUntil = System.currentTimeMillis() + DimDoors.properties.HighlightRiftCoreFor;
    }

    @Override
    public void riftClosing(TileEntityRift rift) {
        ClosingRifts.started(rift);
    }

    @Override
    public void riftReplaced(TileEntityRift rift) {
        ClosingRifts.replaced(rift);
    }

    @Override
    public void registerSidedHooks(DDProperties properties) {
        ClientOnlyHooks hooks = new ClientOnlyHooks(properties);
        MinecraftForge.EVENT_BUS.register(hooks);
        MinecraftForge.EVENT_BUS.register(new MonolithEyeGlow());
        LimboFogCommand.register();
        FMLCommonHandler.instance()
            .bus()
            .register(new MonolithAggroEffect());
        if (Loader.isModLoaded("distanthorizons")) FMLCommonHandler.instance()
            .bus()
            .register(new DistantHorizonsInLimbo());
        MinecraftForge.TERRAIN_GEN_BUS.register(hooks);
        PocketManager.getDimwatcher()
            .registerReceiver(new PocketManager.ClientDimWatcher());
        PocketManager.getLinkWatcher()
            .registerReceiver(new PocketManager.ClientLinkWatcher());
    }

    @Override
    public EntityPlayer getMessagePlayer(MessageContext ctx) {
        return Minecraft.getMinecraft().thePlayer;
    }
}
