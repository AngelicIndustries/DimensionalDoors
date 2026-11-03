package org.dimdev.dimdoors.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/** Draws a falling dripstone the way Et Futurum draws it standing: two crossed squares. */
public class RenderFallingDripstone extends Render {

    private final RenderBlocks renderBlocks = new RenderBlocks();

    public RenderFallingDripstone() {
        shadowSize = 0.0F;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        EntityFallingBlock falling = (EntityFallingBlock) entity;
        Block block = falling.func_145805_f();
        if (block == null) return;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        bindEntityTexture(entity);
        GL11.glDisable(GL11.GL_LIGHTING);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setBrightness(entity.getBrightnessForRender(partialTicks));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        renderBlocks.drawCrossedSquares(block.getIcon(0, falling.field_145814_a), -0.5, -0.5, -0.5, 1.0F);
        tessellator.draw();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TextureMap.locationBlocksTexture;
    }
}
