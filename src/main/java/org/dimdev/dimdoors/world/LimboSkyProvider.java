package org.dimdev.dimdoors.world;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

/**
 * Limbo's sky: an open eye overhead and a closed one below, as in later Dimensional Doors, whatever Limbo's clock says
 * (Dark Limbo stops it at midnight, which turned them over). The eye overhead is alive. Now and then it blinks: its
 * lids narrow it to a slit, it is shut a moment, and it opens again, more slowly. Its pupil mostly rests in the middle,
 * drifting a little; unwatched, it keeps slowly looking aside, mostly sideways, holds there, often looks somewhere
 * else,
 * and comes back. Look up at it and it turns back to you, quickly but visibly, and seldom looks away while watched.
 */
public class LimboSkyProvider extends CustomSkyProvider {

    private static final ResourceLocation LIDS = new ResourceLocation("DimDoors:textures/other/limboEyeLids.png");
    private static final ResourceLocation PUPIL = new ResourceLocation("DimDoors:textures/other/limboEyePupil.png");
    private static final ResourceLocation SHUT = new ResourceLocation("DimDoors:textures/other/limboMoon.png");
    /** The eye textures' size, in pixels. */
    private static final float TEXTURE = 128;

    private static final int CLOSING_MS = 120, SHUT_MS = 100, OPENING_MS = 240;
    private static final int MIN_BLINK_GAP_MS = 4000, MAX_BLINK_GAP_MS = 14000;
    /** Narrower than this, the eye is drawn shut. */
    private static final float SLIT = 0.15F;

    // How far the pupil may look, in texture pixels from the middle, keeping its ring inside the lids: furthest
    // sideways, and up or down there, where the lids are widest; nearer the corners there is less room
    private static final float REACH_SIDE = 21, REACH_UP = 9.5F, REACH_DOWN = 7.5F;
    /** Time between looks aside while unwatched, and while watched. */
    private static final int MIN_REST_MS = 1200, MAX_REST_MS = 4000, MIN_WATCHED_REST_MS = 3000,
        MAX_WATCHED_REST_MS = 9000;
    private static final int MIN_HOLD_MS = 900, MAX_HOLD_MS = 2500;
    /** A look aside takes this long, and this much more for every pixel it goes. */
    private static final int LOOK_MS = 260, LOOK_MS_PER_PIXEL = 9;
    private static final int RETURN_MS = 700, NOTICE_MS = 400;
    /** The chance it looks somewhere else instead of coming back, and that it looks away while watched. */
    private static final float LOOK_ON = 0.45F, LOOK_AWAY_WATCHED = 0.3F;
    /** A player looking up at least this steeply watches the eye. */
    private static final float WATCHED_PITCH = -50;

    private final Random random = new Random();
    private long blinkAt = -1;
    /** How open the eye overhead is this frame, from 0 (shut) to 1. */
    private float open = 1;

    private float fromX, fromY, toX, toY, gazeX, gazeY;
    private long moveAt, nextLook = -1;
    private int moveMs = 1;
    private boolean away, snappy;

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        long now = Minecraft.getSystemTime();
        blink(now);
        gaze(now, mc.renderViewEntity != null && mc.renderViewEntity.rotationPitch <= WATCHED_PITCH);
        super.render(partialTicks, world, mc);
    }

    private void blink(long now) {
        if (blinkAt < 0) blinkAt = now + between(MIN_BLINK_GAP_MS, MAX_BLINK_GAP_MS);
        long into = now - blinkAt;
        if (into < 0) open = 1;
        else if (into < CLOSING_MS) open = 1 - smooth(into / (float) CLOSING_MS);
        else if (into < CLOSING_MS + SHUT_MS) open = 0;
        else if (into < CLOSING_MS + SHUT_MS + OPENING_MS) {
            open = smooth((into - CLOSING_MS - SHUT_MS) / (float) OPENING_MS);
        } else {
            open = 1;
            blinkAt = now + between(MIN_BLINK_GAP_MS, MAX_BLINK_GAP_MS);
        }
    }

    private void gaze(long now, boolean watched) {
        if (nextLook < 0) nextLook = now + rest(watched);
        float t = Math.min(1, (now - moveAt) / (float) moveMs);
        // Looks start and end gently; turning back to a watcher is quick from the first moment
        float eased = snappy ? 1 - (1 - t) * (1 - t) : t * t * t * (t * (6 * t - 15) + 10);
        gazeX = fromX + (toX - fromX) * eased;
        gazeY = fromY + (toY - fromY) * eased;
        if (away && watched) {
            move(now, 0, 0, NOTICE_MS, true);
            away = false;
            nextLook = now + NOTICE_MS + rest(true);
        } else if (t >= 1 && now >= nextLook) {
            if (away && random.nextFloat() >= LOOK_ON) {
                move(now, 0, 0, RETURN_MS, false);
                away = false;
                nextLook = now + RETURN_MS + rest(watched);
            } else if (!away && watched && random.nextFloat() >= LOOK_AWAY_WATCHED) {
                nextLook = now + rest(true);
            } else {
                look(now);
            }
        }
        // Never quite still: a slow drift of under a pixel
        gazeX += (float) (0.5 * Math.sin(now / 1300D) + 0.3 * Math.sin(now / 710D + 1));
        gazeY += (float) (0.4 * Math.sin(now / 1700D + 2));
    }

    /** Looks aside: mostly sideways, now and then up or down, as far as the lids leave room. */
    private void look(long now) {
        boolean sideways = random.nextFloat() < 0.75F;
        float x = REACH_SIDE * (sideways ? 0.45F + 0.55F * random.nextFloat() : 0.4F * random.nextFloat());
        if (random.nextBoolean()) x = -x;
        float room = 1 - (x / (REACH_SIDE + 2)) * (x / (REACH_SIDE + 2));
        float y = (random.nextFloat() * 2 - 1) * (sideways ? 0.6F : 1);
        y *= (y < 0 ? REACH_UP : REACH_DOWN) * room;
        float distance = (float) Math.hypot(x - gazeX, y - gazeY);
        int ms = LOOK_MS + (int) (LOOK_MS_PER_PIXEL * distance);
        move(now, x, y, ms, false);
        away = true;
        nextLook = now + ms + between(MIN_HOLD_MS, MAX_HOLD_MS);
    }

    private void move(long now, float x, float y, int ms, boolean snappy) {
        fromX = gazeX;
        fromY = gazeY;
        toX = x;
        toY = y;
        moveAt = now;
        moveMs = ms;
        this.snappy = snappy;
    }

    private int rest(boolean watched) {
        return watched ? between(MIN_WATCHED_REST_MS, MAX_WATCHED_REST_MS) : between(MIN_REST_MS, MAX_REST_MS);
    }

    private int between(int min, int max) {
        return min + random.nextInt(max - min);
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    @Override
    protected float celestialAngle(WorldClient world, float partialTicks) {
        return 0;
    }

    @Override
    protected float sunHeight() {
        return open < SLIT ? 1 : open;
    }

    @Override
    protected void drawOnSun(Minecraft mc, Tessellator tessellator, float halfWidth, float halfHeight) {
        if (open < SLIT) return;
        // Narrowed by the lids as the rest of the eye is
        float x = gazeX * 2 * halfWidth / TEXTURE, z = gazeY * 2 * halfHeight / TEXTURE;
        mc.renderEngine.bindTexture(PUPIL);
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x - halfWidth, 100.0D, z - halfHeight, 0.0D, 0.0D);
        tessellator.addVertexWithUV(x + halfWidth, 100.0D, z - halfHeight, 1.0D, 0.0D);
        tessellator.addVertexWithUV(x + halfWidth, 100.0D, z + halfHeight, 1.0D, 1.0D);
        tessellator.addVertexWithUV(x - halfWidth, 100.0D, z + halfHeight, 0.0D, 1.0D);
        tessellator.draw();
    }

    @Override
    public ResourceLocation getMoonRenderPath() {
        return SHUT;
    }

    @Override
    public ResourceLocation getSunRenderPath() {
        return open < SLIT ? SHUT : LIDS;
    }
}
