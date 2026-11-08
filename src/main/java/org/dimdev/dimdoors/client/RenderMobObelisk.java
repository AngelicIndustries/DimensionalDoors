package org.dimdev.dimdoors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.ticking.MobMonolith;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws a Monolith as vanilla draws a spider: the body, then its open eye glowing in a render pass, which Angelica
 * draws
 * with a shader pack's glowing eyes program (RenderMobObeliskAngelica). Complementary lights and fogs that program's
 * pixels as the body under them, though, so under a shader pack the eye glows again after the world (MonolithEyeGlow).
 * The whole Monolith jitters as its aggro grows.
 */
@SideOnly(Side.CLIENT)
public class RenderMobObelisk extends RenderLiving {

    protected ModelMobObelisk obeliskModel;

    protected RenderMobObelisk(float shadowSize) {
        super(new ModelMobObelisk(), shadowSize);
        this.obeliskModel = (ModelMobObelisk) this.mainModel;
        setRenderPassModel(new ModelMobObelisk());
    }

    /** The renderer, marked as one with an eye pass when Angelica has the API for it. */
    public static RenderMobObelisk create(float shadowSize) {
        return hasAngelicaEyePass() ? RenderMobObeliskAngelica.create(shadowSize) : new RenderMobObelisk(shadowSize);
    }

    /** Whether Angelica is there with its eye pass API (EyePassRenderer). */
    private static boolean hasAngelicaEyePass() {
        try {
            Class.forName(
                "com.gtnewhorizons.angelica.api.EyePassRenderer",
                false,
                RenderMobObelisk.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Override
    public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
        MobMonolith monolith = ((MobMonolith) entity);
        double[] jitter = jitter(monolith);
        super.doRender(entity, x + jitter[0], y + jitter[1], z + jitter[2], yaw, partialTicks);
        if (monolith.getTextureState() > 0 && ShaderPacks.inUse()) MonolithEyeGlow.queue(monolith);
    }

    /** How far a Monolith is shaken from its place right now, growing with its aggro; each has its own shake. */
    static double[] jitter(MobMonolith monolith) {
        final float minScaling = 0;
        final float maxScaling = 0.1f;
        float aggroScaling = 0;
        if (monolith.isDangerous()) {
            // Use linear interpolation to scale how much jitter we want for our given aggro level
            aggroScaling = minScaling + (maxScaling - minScaling) * monolith.getAggroProgress();
        }

        // Calculate jitter - include entity ID to give Monoliths individual jitters
        float time = ((Minecraft.getSystemTime() + 0xF1234568 * monolith.getEntityId()) % 200000) / 50.0F;
        // We use random constants here on purpose just to get different wave forms
        return new double[] { aggroScaling * Math.sin(1.1f * time) * Math.sin(0.8f * time),
            aggroScaling * Math.sin(1.2f * time) * Math.sin(0.9f * time),
            aggroScaling * Math.sin(1.3f * time) * Math.sin(0.7f * time) };
    }

    /** Tilted by the Monolith's pitch, and its model 48 pixels higher than vanilla places one. */
    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        GL11.glRotatef(((MobMonolith) entity).pitchLevel, 1.0F, 0.0F, 0.0F);
        GL11.glTranslatef(0.0F, 48 * 0.0625F, 0.0F);
    }

    /** The body, unlit and blended. */
    @Override
    protected void renderModel(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float ageInTicks,
        float headYaw, float headPitch, float scale) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch, scale);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    /**
     * The glow of an open eye: the model again with only its eyes, at full brightness and added on top, as vanilla
     * draws spider eyes.
     */
    @Override
    protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        MobMonolith monolith = (MobMonolith) entity;
        if (pass > 0 || monolith.getTextureState() == 0) return -1;
        bindTexture(MonolithEyesTexture.of(texture(monolith)));
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glDepthMask(false);
        // Full light, so nothing that darkens Limbo (Dark Limbo, Darkerer) dims the eyes. Vanilla's spider eyes use
        // 61680, which turns black where lightmap coordinates are packed into 16 bits (Angelica)
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        return 1;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return texture((MobMonolith) entity);
    }

    static ResourceLocation texture(MobMonolith monolith) {
        return new ResourceLocation(
            DimDoors.modid + ":textures/mobs/oldMonolith/Monolith" + monolith.getTextureState() + ".png");
    }
}
