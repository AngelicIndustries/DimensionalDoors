package org.dimdev.dimdoors.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

/** Vanilla's door renderer with the block's color applied, so door variants can be tinted. */
public class DoorVariantRender implements ISimpleBlockRenderingHandler {

    private final int renderId;

    public DoorVariantRender(int renderId) {
        this.renderId = renderId;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {}

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (world.getBlock(x, (metadata & 8) != 0 ? y - 1 : y + 1, z) != block) {
            return false;
        }
        int color = block.colorMultiplier(world, x, y, z);
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.instance;
        int brightness = block.getMixedBrightnessForBlock(world, x, y, z);

        tessellator.setBrightness(
            renderer.renderMinY > 0.0D ? brightness : block.getMixedBrightnessForBlock(world, x, y - 1, z));
        tessellator.setColorOpaque_F(0.5F * red, 0.5F * green, 0.5F * blue);
        renderer.renderFaceYNeg(block, x, y, z, renderer.getBlockIcon(block, world, x, y, z, 0));
        tessellator.setBrightness(
            renderer.renderMaxY < 1.0D ? brightness : block.getMixedBrightnessForBlock(world, x, y + 1, z));
        tessellator.setColorOpaque_F(red, green, blue);
        renderer.renderFaceYPos(block, x, y, z, renderer.getBlockIcon(block, world, x, y, z, 1));
        tessellator.setBrightness(
            renderer.renderMinZ > 0.0D ? brightness : block.getMixedBrightnessForBlock(world, x, y, z - 1));
        tessellator.setColorOpaque_F(0.8F * red, 0.8F * green, 0.8F * blue);
        renderer.renderFaceZNeg(block, x, y, z, renderer.getBlockIcon(block, world, x, y, z, 2));
        renderer.flipTexture = false;
        tessellator.setBrightness(
            renderer.renderMaxZ < 1.0D ? brightness : block.getMixedBrightnessForBlock(world, x, y, z + 1));
        tessellator.setColorOpaque_F(0.8F * red, 0.8F * green, 0.8F * blue);
        renderer.renderFaceZPos(block, x, y, z, renderer.getBlockIcon(block, world, x, y, z, 3));
        renderer.flipTexture = false;
        tessellator.setBrightness(
            renderer.renderMinX > 0.0D ? brightness : block.getMixedBrightnessForBlock(world, x - 1, y, z));
        tessellator.setColorOpaque_F(0.6F * red, 0.6F * green, 0.6F * blue);
        renderer.renderFaceXNeg(block, x, y, z, renderer.getBlockIcon(block, world, x, y, z, 4));
        renderer.flipTexture = false;
        tessellator.setBrightness(
            renderer.renderMaxX < 1.0D ? brightness : block.getMixedBrightnessForBlock(world, x + 1, y, z));
        tessellator.setColorOpaque_F(0.6F * red, 0.6F * green, 0.6F * blue);
        renderer.renderFaceXPos(block, x, y, z, renderer.getBlockIcon(block, world, x, y, z, 5));
        renderer.flipTexture = false;
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
