package org.dimdev.dimdoors.client;

import static org.lwjgl.opengl.GL11.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.tileentities.RiftClosing;
import org.dimdev.dimdoors.tileentities.TileEntityRift;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws an open rift as on the 1.12 rewrite (MIT): a black fractal crack that grows with the rift, shakes and
 * flutters, and a spinning tesseract at its core, shown always or for a while after a rift tool misses.
 */
@SideOnly(Side.CLIENT)
public class RenderRift extends TileEntitySpecialRenderer {

    private static final ResourceLocation TESSERACT = new ResourceLocation(
        DimDoors.modid,
        "textures/other/tesseract.png");
    /** How far a core shakes, in blocks. */
    private static final double CORE_SHAKE = 0.05;
    /** Until when (system milliseconds) every rift shows its core. */
    public static long showCoresUntil;

    @Override
    public void renderTileEntityAt(TileEntity entity, double x, double y, double z, float partialTicks) {
        if (!(entity instanceof TileEntityRift)) return;
        TileEntityRift rift = (TileEntityRift) entity;
        if (DimDoors.properties.ShowRiftCore || System.currentTimeMillis() <= showCoresUntil) {
            renderCore(rift, x, y, z, partialTicks);
        }
        double size = rift.crackSize(), twist = 0;
        if (rift.shouldClose) {
            RiftClosing closing = rift.closing();
            double seconds = rift.closeSeconds();
            size *= closing.scale(seconds);
            twist = closing.twist(seconds);
        }
        drawCrack(rift.riftRotation, rift.curveId, size, twist, x + 0.5, y + 0.5, z + 0.5);
    }

    /** Draws a rift's crack, turned by {@code twist} radians in its own plane, one way or the other by its shape. */
    static void drawCrack(float rotation, int curveId, double size, double twist, double x, double y, double z) {
        if (size <= 0) return;
        glPushMatrix();
        // Both sides, no texture or lighting
        glDisable(GL_LIGHTING);
        glDisable(GL_CULL_FACE);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        drawCrack(rotation, RiftCurves.get(curveId), size, (curveId & 1) == 0 ? twist : -twist, x, y, z);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1, 1, 1, 1);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_CULL_FACE);
        glEnable(GL_LIGHTING);
        glPopMatrix();
    }

    private void renderCore(TileEntityRift rift, double x, double y, double z, float partialTicks) {
        rift.renderAngle = (rift.renderAngle + 5 * partialTicks) % 360;
        glPushMatrix();
        glDisable(GL_LIGHTING);
        glDisable(GL_CULL_FACE);
        bindTexture(TESSERACT);
        // A small shake, like the crack's at its quickest, from the position so cores don't shake in step
        double time = (Minecraft.getSystemTime() + (long) (0xF1234568L * (x + y * (2L << 21) + z * (2L << 42))))
            % 2000000;
        double shake = CORE_SHAKE * DimDoors.properties.RiftCoreJitter, pace = 8 * 0.014;
        glTranslated(
            x + 0.5 + shake * Math.sin(1.1 * time * pace) * Math.sin(0.8 * time * 0.014),
            y + 0.5 + shake * Math.sin(1.2 * time * pace) * Math.sin(0.9 * time * 0.014),
            z + 0.5 + shake * Math.sin(1.3 * time * pace) * Math.sin(0.7 * time * 0.014));
        glScaled(0.25, 0.25, 0.25);
        double angle = Math.toRadians(rift.renderAngle);
        Tesseract.draw(1, 0.5F, 1, 1, angle);
        // It glows: a faint copy a little bigger, added onto whatever is behind it
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        glDepthMask(false);
        glScaled(1.2, 1.2, 1.2);
        Tesseract.draw(1, 0.5F, 1, 0.35F, angle);
        glDepthMask(true);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
        glEnable(GL_LIGHTING);
        glPopMatrix();
    }

    private static void drawCrack(float rotation, RiftCurves.Curve curve, double size, double twist, double xWorld,
        double yWorld, double zWorld) {
        double scale = size / (curve.maxX - curve.minX);
        // The middle of the curve's bounding box
        double offsetX = (curve.maxX + curve.minX) / 2D;
        double offsetY = (curve.maxY + curve.minY) / 2D;
        // Shaking, from the position so rifts don't shake in step
        float jitterSpeed = 0.014F;
        long riftRandom = (long) (0xF1234568L * (xWorld + yWorld * (2L << 21) + zWorld * (2L << 42)));
        float time = (Minecraft.getSystemTime() + riftRandom) % 2000000;
        double jitterScale = DimDoors.properties.RiftJitter * TileEntityRift.CRACK_SHAKE * size;
        // Bigger cracks shake faster, up to an 8-block one's pace; past that 1.12's just flickered
        double pace = Math.min(size, 8);
        double xJitter = jitterScale * Math.sin(1.1F * time * pace * jitterSpeed) * Math.sin(0.8F * time * jitterSpeed);
        double yJitter = jitterScale * Math.sin(1.2F * time * pace * jitterSpeed) * Math.sin(0.9F * time * jitterSpeed);
        double zJitter = jitterScale * Math.sin(1.3F * time * pace * jitterSpeed) * Math.sin(0.7F * time * jitterSpeed);
        // The triangles flutter, most visibly at the edges
        float flutterMagnitude = 0.6F;
        int flutterModulo = 10;
        float flutterSpeed = 0.014F;
        double[] flutters = new double[flutterModulo];
        for (int i = 0; i < flutterModulo; i++) {
            flutters[i] = Math.sin((1F + i / 10F) * time * flutterSpeed) * Math.cos(1F - i / 10F * time * flutterSpeed)
                * flutterMagnitude;
        }
        double cos = Math.cos(Math.toRadians(rotation)), sin = Math.sin(Math.toRadians(rotation));
        double twistCos = Math.cos(twist), twistSin = Math.sin(twist);
        // Nearly black: it darkens whatever is behind it
        glColor4f(0.08F, 0.08F, 0.08F, 0.3F);
        glBlendFunc(GL_ONE_MINUS_DST_COLOR, GL_ZERO);
        glBegin(GL_TRIANGLES);
        for (int i = 0; i < curve.xs.length; i++) {
            int px = curve.xs[i], py = curve.ys[i];
            // Keeps most triangles in the middle from fluttering into each other
            int flutterIndex = Math.abs((px + py) * (px + py + 1) / 2 + py);
            double cx = px + flutters[(flutterIndex + 1) % flutterModulo] - offsetX;
            double cy = py + flutters[flutterIndex % flutterModulo] - offsetY;
            double fz = flutters[(flutterIndex + 2) % flutterModulo];
            // Twisted in its plane, then turned about the rift; 1.12 left the centring out of z, which put turned
            // cracks beside their rift
            double fx = cx * twistCos - cy * twistSin;
            double vx = fx * cos - fz * sin;
            double vy = cx * twistSin + cy * twistCos;
            double vz = fx * sin + fz * cos;
            glVertex3d(xWorld + vx * scale + xJitter, yWorld + vy * scale + yJitter, zWorld + vz * scale + zJitter);
        }
        glEnd();
    }
}
