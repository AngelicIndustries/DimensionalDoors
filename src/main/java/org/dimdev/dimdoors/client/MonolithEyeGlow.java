package org.dimdev.dimdoors.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.dimdev.dimdoors.ticking.MobMonolith;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The glow of a Monolith's open eye under a shader pack: the eye in grey on the Monolith's front face, tinted red,
 * drawn
 * after the world. A shader pack draws the world first and then fogs it by depth, so the glowing eyes pass, drawn with
 * the body, comes out lit and fogged as the body is (Complementary); drawn after that, the eye glows and stays in sight
 * as far as Eyes in the Shadows' eyes, and blocks in front still hide it. RenderMobObelisk queues each Monolith it
 * draws
 * with its eye open; the glow follows it, jitter included.
 */
@SideOnly(Side.CLIENT)
public final class MonolithEyeGlow {

    private static final List<MobMonolith> QUEUED = new ArrayList<>();
    /** For the front face's corners and texture coordinates. */
    private static final ModelMobObelisk MODEL = new ModelMobObelisk();
    private static final float PIXEL = 0.0625F;
    /** Out from the face by this much, so the body doesn't cover the glow. */
    private static final float LIFT = 0.02F;
    /** Added on top of itself this many times, so it is bright enough to bloom. */
    private static final int LAYERS = 2;

    static void queue(MobMonolith monolith) {
        QUEUED.add(monolith);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        for (MobMonolith monolith : QUEUED) draw(monolith, event.partialTicks);
        QUEUED.clear();
    }

    private static void draw(MobMonolith monolith, float partialTicks) {
        double[] jitter = RenderMobObelisk.jitter(monolith);
        double x = monolith.lastTickPosX + (monolith.posX - monolith.lastTickPosX) * partialTicks
            - RenderManager.renderPosX;
        double y = monolith.lastTickPosY + (monolith.posY - monolith.lastTickPosY) * partialTicks
            - RenderManager.renderPosY;
        double z = monolith.lastTickPosZ + (monolith.posZ - monolith.lastTickPosZ) * partialTicks
            - RenderManager.renderPosZ;
        float yaw = monolith.prevRenderYawOffset
            + wrap(monolith.renderYawOffset - monolith.prevRenderYawOffset) * partialTicks;

        GL11.glPushMatrix();
        // Placed as RenderMobObelisk places the model
        GL11.glTranslated(x + jitter[0], y + jitter[1], z + jitter[2]);
        GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(-1.0F, -1.0F, 1.0F);
        GL11.glRotatef(monolith.pitchLevel, 1.0F, 0.0F, 0.0F);
        GL11.glTranslatef(0.0F, 24 * PIXEL - 0.0078125F, 0.0F);
        float size = monolith.getRenderSizeModifier();
        GL11.glScalef(size, size, size);

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(MonolithEyesTexture.mask(RenderMobObelisk.texture(monolith)));
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);
        float lastX = OpenGlHelper.lastBrightnessX, lastY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

        // The front (north) face, with the corners and texture coordinates ModelBox gives it (texture offset 0, 0),
        // tinted as the Monolith's red particles are
        ModelBox box = (ModelBox) MODEL.wholemonolith.cubeList.get(0);
        float w = box.posX2 - box.posX1, h = box.posY2 - box.posY1, d = box.posZ2 - box.posZ1;
        float u1 = d / MODEL.textureWidth, u2 = (d + w) / MODEL.textureWidth;
        float v1 = d / MODEL.textureHeight, v2 = (d + h) / MODEL.textureHeight;
        float x1 = box.posX1 * PIXEL, x2 = box.posX2 * PIXEL, y1 = box.posY1 * PIXEL, y2 = box.posY2 * PIXEL;
        float front = box.posZ1 * PIXEL - LIFT;
        Tessellator tessellator = Tessellator.instance;
        for (int layer = 0; layer < LAYERS; layer++) {
            tessellator.startDrawingQuads();
            tessellator.setColorRGBA_F(1.0F, 0.1F, 0.1F, 1.0F);
            tessellator.addVertexWithUV(x2, y1, front, u2, v1);
            tessellator.addVertexWithUV(x1, y1, front, u1, v1);
            tessellator.addVertexWithUV(x1, y2, front, u1, v2);
            tessellator.addVertexWithUV(x2, y2, front, u2, v2);
            tessellator.draw();
        }

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private static float wrap(float degrees) {
        while (degrees < -180.0F) degrees += 360.0F;
        while (degrees >= 180.0F) degrees -= 360.0F;
        return degrees;
    }
}
