package org.dimdev.dimdoors.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;

import org.dimdev.dimdoors.blocks.BlockFadebloom;
import org.dimdev.dimdoors.config.DDProperties;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

/** The Fadebloom as a flower: its stem lit like any plant, its petals at least as bright as Fadebloom Glow. */
public class FadebloomRender implements ISimpleBlockRenderingHandler {

    private final int renderId;

    public FadebloomRender(int renderId) {
        this.renderId = renderId;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {}

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        BlockFadebloom orchid = (BlockFadebloom) block;
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        // Breaking cracks are drawn once, over the whole plant
        if (renderer.hasOverrideBlockTexture()) {
            renderer.drawCrossedSquares(renderer.overrideBlockTexture, x, y, z, 1.0F);
            return true;
        }
        renderer.drawCrossedSquares(orchid.stemIcon(), x, y, z, 1.0F);
        // The petals are lit at least like Fadebloom Glow's light level, whatever the light around them
        int brightness = block.getMixedBrightnessForBlock(world, x, y, z);
        int glow = DDProperties.instance().FadebloomGlow << 4;
        tessellator.setBrightness(brightness & 0xFFFF0000 | Math.max(brightness & 0xFFFF, glow));
        renderer.drawCrossedSquares(orchid.petalIcon(), x, y, z, 1.0F);
        return true;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return false;
    }

    @Override
    public int getRenderId() {
        return renderId;
    }
}
