package org.dimdev.dimdoors.client;

import java.util.Arrays;
import java.util.Random;

/**
 * The shape of a door's or trapdoor's portal for one frame, in the opening's plane: h across and v up from its
 * middle. A rectangle with rounded corners, or an oval (see update), whose edge is a fractal (bulges on bulges, each
 * row twice as many and about two thirds as tall) turning slowly around it, shaken a little, with a glowing rim whose
 * outer edge flickers in spikes turning the other way. Now and then a glitch cuts it into bands and shifts them
 * sideways. All of it is quads, as shader packs expect.
 */
final class PortalShape {

    /** Per fill vertex: h and v of its spot on the portal (where its texture is), and h and v where it is drawn. */
    static final int FILL_STRIDE = 4;
    /**
     * Per rim vertex: the same, then its glow (0 to 1), the turn around the portal (0 to 1) and how far out (0 to 1).
     */
    static final int RIM_STRIDE = 7;

    private static final int POINTS = 384;
    /** How far the portal keeps from the opening's sides, in blocks. */
    private static final double MARGIN = 0.1;
    /** The steepest superellipse, standing in for a sharp rectangle, and how finely its outline is measured. */
    private static final double MAX_EXPONENT = 400;
    private static final int OUTLINE_STEPS = 8192;
    /** One band covering everything, unshifted, for when no glitch cuts the portal. */
    private static final double[][] UNBROKEN = { { 0, 0, 0 } };
    /** The edge's rows of bulges: how many fit around the portal, and how far each moves the edge, in blocks. */
    private static final int[] WAVES = { 8, 16, 32, 64 };
    private static final double[] HEIGHTS = { 0.022, 0.015, 0.01, 0.0065 };
    /** How fast the edge turns around the portal, and how fast each row drifts on its own, in radians a second. */
    private static final double SPIN = 0.8, DRIFT = 0.4;
    /** The rim reaches this far inside the edge, and up to this far outside it, in blocks. */
    private static final double RIM_IN = 0.035, RIM_OUT = 0.09;
    /** How fast the rim's flames turn, the other way round. */
    private static final double FLAME_SPIN = -0.9;
    /** A new shake every this many milliseconds, moving the portal and its radius up to this far, in blocks. */
    private static final long SHAKE_MS = 50;
    private static final double SHAKE = 0.008, PULSE = 0.012;
    /** A glitch may start once in this many milliseconds, by this chance, and lasts this long. */
    private static final long GLITCH_WINDOW_MS = 3500, GLITCH_STEP_MS = 45;
    private static final float GLITCH_CHANCE = 0.55F;
    private static final int GLITCH_MIN_MS = 90, GLITCH_MORE_MS = 160;
    /** How far a glitch shifts a band, in blocks, and how often it drops one no taller than the drop height. */
    private static final double GLITCH_SHIFT = 0.09, GLITCH_DROP_HEIGHT = 0.3;
    private static final float GLITCH_DROP = 0.3F;

    final Quads fill = new Quads(FILL_STRIDE), rim = new Quads(RIM_STRIDE);
    /** Extra brightness while a glitch flashes, 0 to about 0.5. */
    float flash;

    /** The edge, and the rim's inner and outer rings, POINTS + 1 of each so the last closes the loop. */
    private final double[] edge = new double[2 * (POINTS + 1)], inner = new double[edge.length],
        outer = new double[edge.length];
    /**
     * The outline before its bulges, in even steps along it: for each point, the spot on the middle line it bulges
     * away from, the way it bulges and how far out it lies. Those of the last few sizes are kept, as a renderer
     * draws portals of a few sizes (open and shut).
     */
    private double[] spine, away, extent;
    private final Outline[] outlines = new Outline[4];
    private int nextOutline;
    private final double[] quad = new double[8], rimIn = new double[20], rimOut = new double[20];
    private final double[] edgePhase = new double[WAVES.length], flamePhase = new double[WAVES.length];

    /**
     * Made into the portal at a time, inside an opening from left to right and bottom to top (from the doorway's
     * middle), its own per seed: a rectangle at
     * roundness 0, an oval at 1, and rectangles with ever rounder corners between. Roughness scales the edge's bulges
     * (0 is a smooth edge) and spin speed how fast the edge and the rim's spikes turn and drift (0 keeps them still).
     */
    void update(long seed, long millis, double left, double right, double bottom, double top, double roundness,
        double roughness, double spinSpeed) {
        fill.clear();
        rim.clear();
        flash = 0;
        outline((right - left) / 2, (top - bottom) / 2, roundness);
        double time = millis / 1000.0, turning = time * spinSpeed;
        Random shake = new Random(mix(seed, millis / SHAKE_MS));
        double ch = (shake.nextDouble() * 2 - 1) * SHAKE, cv = (shake.nextDouble() * 2 - 1) * SHAKE;
        // The middle of the portal, shaken
        double mh = (left + right) / 2 + ch, mv = (bottom + top) / 2 + cv;
        double pulse = (shake.nextDouble() * 2 - 1) * PULSE;
        Random phases = new Random(seed);
        for (int k = 0; k < WAVES.length; k++) {
            edgePhase[k] = phases.nextDouble() * Math.PI * 2;
            flamePhase[k] = phases.nextDouble() * Math.PI * 2;
        }

        for (int i = 0; i <= POINTS; i++) {
            double angle = Math.PI * 2 * (i % POINTS) / POINTS;
            double sh = spine[2 * i] + mh, sv = spine[2 * i + 1] + mv, uh = away[2 * i], uv = away[2 * i + 1];
            double reach = reach(sh, sv, uh, uv, left, right, bottom, top);
            double r = extent[i] + pulse + roughness * waves(angle, turning, SPIN, edgePhase, false);
            double flame = 0.5 + 0.5 * roughness * waves(angle, turning, FLAME_SPIN, flamePhase, true) / HEIGHTS[0];
            double out = r + RIM_OUT * (0.4 + 0.6 * Math.max(0, Math.min(1, flame)));
            put(edge, i, sh, sv, uh, uv, Math.min(r, reach));
            put(inner, i, sh, sv, uh, uv, Math.min(r - RIM_IN, reach));
            put(outer, i, sh, sv, uh, uv, Math.min(out, reach));
        }

        double[][] bands = glitchBands(seed, millis, bottom, top);
        for (double[] band : bands == null ? UNBROKEN : bands) {
            double[] bounds = bands == null ? null : new double[] { band[0], band[1], left - band[2], right - band[2] };
            for (int i = 0; i < POINTS; i++) {
                // The fill between the middle line and the edge
                quad[0] = spine[2 * i] + mh;
                quad[1] = spine[2 * i + 1] + mv;
                System.arraycopy(edge, 2 * i, quad, 2, 4);
                quad[6] = spine[2 * i + 2] + mh;
                quad[7] = spine[2 * i + 3] + mv;
                double[] piece = bounds == null ? quad : clip(quad, 2, bounds);
                if (piece != null) addFan(fill, piece, 2, piece.length / 2, band[2], ch, cv);
                addRimQuads(i, bounds, band[2], ch, cv);
            }
        }
    }

    /**
     * Picks the outline before its bulges, working it out for a new size or roundness: a superellipse
     * (|h / a|^n + |v / b|^n = 1, n = 2 / roundness). Its points are even steps along it, so the bulges are even all
     * around. Each bulges away from the nearest spot on a middle line along the long side (as long as the long side
     * less the short), so the sides bulge straight out and the ends round about their middle, and no two bulges
     * cross.
     */
    private void outline(double halfWidth, double halfHeight, double roundness) {
        for (Outline outline : outlines) {
            if (outline != null && outline.matches(halfWidth, halfHeight, roundness)) {
                spine = outline.spine;
                away = outline.away;
                extent = outline.extent;
                return;
            }
        }
        Outline outline = new Outline(halfWidth, halfHeight, roundness);
        outlines[nextOutline] = outline;
        nextOutline = (nextOutline + 1) % outlines.length;
        spine = outline.spine;
        away = outline.away;
        extent = outline.extent;
        double a = halfWidth - MARGIN, b = halfHeight - MARGIN;
        double power = roundness <= 2 / MAX_EXPONENT ? 2 / MAX_EXPONENT : roundness;
        double[] dense = new double[2 * (OUTLINE_STEPS + 1)], along = new double[OUTLINE_STEPS + 1];
        for (int j = 0; j <= OUTLINE_STEPS; j++) {
            double t = Math.PI * 2 * j / OUTLINE_STEPS;
            dense[2 * j] = a * superPow(Math.cos(t), power);
            dense[2 * j + 1] = b * superPow(Math.sin(t), power);
            if (j > 0) along[j] = along[j - 1]
                + Math.hypot(dense[2 * j] - dense[2 * j - 2], dense[2 * j + 1] - dense[2 * j - 1]);
        }
        double spineH = Math.max(0, a - b), spineV = Math.max(0, b - a);
        for (int i = 0, j = 0; i <= POINTS; i++) {
            double target = along[OUTLINE_STEPS] * i / POINTS;
            while (j < OUTLINE_STEPS - 1 && along[j + 1] < target) j++;
            double f = (target - along[j]) / Math.max(1.0E-9, along[j + 1] - along[j]);
            double h = dense[2 * j] + (dense[2 * j + 2] - dense[2 * j]) * f;
            double v = dense[2 * j + 1] + (dense[2 * j + 3] - dense[2 * j + 1]) * f;
            spine[2 * i] = Math.max(-spineH, Math.min(spineH, h));
            spine[2 * i + 1] = Math.max(-spineV, Math.min(spineV, v));
            extent[i] = Math.hypot(h - spine[2 * i], v - spine[2 * i + 1]);
            away[2 * i] = (h - spine[2 * i]) / extent[i];
            away[2 * i + 1] = (v - spine[2 * i + 1]) / extent[i];
        }
    }

    /**
     * The fractal at a turn around the portal (in radians), turning at the spin: rows of round bulges meeting in sharp
     * dips, or turned over, sharp spikes. Averages 0.
     */
    private static double waves(double angle, double time, double spin, double[] phase, boolean spikes) {
        double sum = 0;
        for (int k = 0; k < WAVES.length; k++) {
            double drift = (k % 2 == 0 ? DRIFT : -DRIFT) * time;
            double bulge = Math.abs(Math.sin((WAVES[k] * (angle - spin * time) + phase[k] + drift) / 2)) - 2 / Math.PI;
            sum += HEIGHTS[k] * 2 * (spikes ? -bulge : bulge);
        }
        return sum;
    }

    /**
     * x to the power, keeping its sign. A cosine or sine that should be 0 comes out a hair off it, which a small
     * power (a sharp corner) would blow up to most of the way out.
     */
    private static double superPow(double x, double power) {
        return Math.abs(x) < 1.0E-9 ? 0 : Math.copySign(Math.pow(Math.abs(x), power), x);
    }

    /** How far from a spot along a direction the opening reaches. */
    private static double reach(double sh, double sv, double uh, double uv, double left, double right, double bottom,
        double top) {
        double reach = Double.MAX_VALUE;
        if (uh > 0) reach = (right - sh) / uh;
        else if (uh < 0) reach = (left - sh) / uh;
        if (uv > 0) reach = Math.min(reach, (top - sv) / uv);
        else if (uv < 0) reach = Math.min(reach, (bottom - sv) / uv);
        return reach;
    }

    private static void put(double[] ring, int i, double sh, double sv, double uh, double uv, double r) {
        ring[2 * i] = sh + uh * r;
        ring[2 * i + 1] = sv + uv * r;
    }

    /**
     * The bands a glitch cuts the portal into right now, each {v from, v to, h shift}, or null when it isn't
     * glitching. Sets the flash.
     */
    private double[][] glitchBands(long seed, long millis, double bottom, double top) {
        long window = millis / GLITCH_WINDOW_MS;
        Random random = new Random(mix(seed ^ 0x6C1D7C4AL, window));
        if (random.nextFloat() >= GLITCH_CHANCE) return null;
        long start = window * GLITCH_WINDOW_MS
            + random.nextInt((int) GLITCH_WINDOW_MS - GLITCH_MIN_MS - GLITCH_MORE_MS);
        long end = start + GLITCH_MIN_MS + random.nextInt(GLITCH_MORE_MS);
        if (millis < start || millis >= end) return null;
        random = new Random(mix(seed ^ 0x3A9B21F5L, millis / GLITCH_STEP_MS));
        int cuts = 2 + random.nextInt(4);
        double[] at = new double[cuts + 2];
        at[0] = bottom - 1;
        at[1] = top + 1;
        for (int i = 2; i < at.length; i++) at[i] = bottom + random.nextDouble() * (top - bottom);
        Arrays.sort(at);
        double[][] bands = new double[cuts + 1][];
        int count = 0;
        for (int i = 0; i <= cuts; i++) {
            // Only thin bands drop out
            if (at[i + 1] - at[i] < GLITCH_DROP_HEIGHT && random.nextFloat() < GLITCH_DROP) continue;
            double shift = random.nextFloat() < 0.35F ? 0 : (random.nextDouble() * 2 - 1) * GLITCH_SHIFT;
            bands[count++] = new double[] { at[i], at[i + 1], shift };
        }
        flash = random.nextFloat() * 0.5F;
        return Arrays.copyOf(bands, count);
    }

    /** The rim's two quads at a point: inside the edge, glowing toward it, and outside, fading out. */
    private void addRimQuads(int i, double[] bounds, double shift, double ch, double cv) {
        double turn = (double) i / POINTS, next = (double) (i + 1) / POINTS;
        rimVertex(rimIn, 0, inner, i, 0, turn, 0);
        rimVertex(rimIn, 1, edge, i, 1, turn, 0.5);
        rimVertex(rimIn, 2, edge, i + 1, 1, next, 0.5);
        rimVertex(rimIn, 3, inner, i + 1, 0, next, 0);
        rimVertex(rimOut, 0, edge, i, 1, turn, 0.5);
        rimVertex(rimOut, 1, outer, i, 0, turn, 1);
        rimVertex(rimOut, 2, outer, i + 1, 0, next, 1);
        rimVertex(rimOut, 3, edge, i + 1, 1, next, 0.5);
        addRimQuad(rimIn, bounds, shift, ch, cv);
        addRimQuad(rimOut, bounds, shift, ch, cv);
    }

    private void addRimQuad(double[] quad, double[] bounds, double shift, double ch, double cv) {
        double[] piece = bounds == null ? quad : clip(quad, 5, bounds);
        if (piece != null) addFan(rim, piece, 5, piece.length / 5, shift, ch, cv);
    }

    private static void rimVertex(double[] quad, int corner, double[] ring, int i, double glow, double turn,
        double out) {
        int at = corner * 5;
        quad[at] = ring[2 * i];
        quad[at + 1] = ring[2 * i + 1];
        quad[at + 2] = glow;
        quad[at + 3] = turn;
        quad[at + 4] = out;
    }

    /**
     * A convex polygon (or a fan around its first vertex) as quads: each of its vertices is h, v and then stride - 2
     * more values, drawn shifted along h.
     */
    private void addFan(Quads quads, double[] polygon, int stride, int count, double shift, double ch, double cv) {
        for (int k = 1; k + 1 < count; k += 2) {
            int last = Math.min(k + 2, count - 1);
            addVertex(quads, polygon, stride, 0, shift, ch, cv);
            addVertex(quads, polygon, stride, k, shift, ch, cv);
            addVertex(quads, polygon, stride, k + 1, shift, ch, cv);
            addVertex(quads, polygon, stride, last, shift, ch, cv);
        }
    }

    private static void addVertex(Quads quads, double[] polygon, int stride, int i, double shift, double ch,
        double cv) {
        int at = quads.grow();
        double h = polygon[i * stride], v = polygon[i * stride + 1];
        quads.data[at] = h - ch;
        quads.data[at + 1] = v - cv;
        quads.data[at + 2] = h + shift;
        quads.data[at + 3] = v;
        System.arraycopy(polygon, i * stride + 2, quads.data, at + 4, stride - 2);
    }

    /**
     * A convex polygon (vertices of h, v and more values) cut to bounds {v from, v to, h from, h to}, or null when
     * nothing is left.
     */
    private static double[] clip(double[] polygon, int stride, double[] bounds) {
        polygon = clip(polygon, stride, 1, bounds[0], true);
        if (polygon != null) polygon = clip(polygon, stride, 1, bounds[1], false);
        if (polygon != null) polygon = clip(polygon, stride, 0, bounds[2], true);
        if (polygon != null) polygon = clip(polygon, stride, 0, bounds[3], false);
        return polygon;
    }

    /** One step of Sutherland-Hodgman: keeps the part where the axis value is above (or below) the bound. */
    private static double[] clip(double[] polygon, int stride, int axis, double bound, boolean above) {
        int count = polygon.length / stride;
        double[] out = new double[(count * 2) * stride];
        int size = 0;
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            double di = polygon[i * stride + axis] - bound, dj = polygon[j * stride + axis] - bound;
            if (!above) {
                di = -di;
                dj = -dj;
            }
            if (di >= 0) {
                System.arraycopy(polygon, i * stride, out, size * stride, stride);
                size++;
            }
            if ((di >= 0) != (dj >= 0)) {
                double t = di / (di - dj);
                for (int c = 0; c < stride; c++) {
                    out[size * stride + c] = polygon[i * stride + c]
                        + (polygon[j * stride + c] - polygon[i * stride + c]) * t;
                }
                size++;
            }
        }
        return size < 3 ? null : Arrays.copyOf(out, size * stride);
    }

    private static long mix(long seed, long n) {
        long z = seed * 0x9E3779B97F4A7C15L + n * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static final class Outline {

        final double halfWidth, halfHeight, roundness;
        final double[] spine = new double[2 * (POINTS + 1)], away = new double[2 * (POINTS + 1)],
            extent = new double[POINTS + 1];

        Outline(double halfWidth, double halfHeight, double roundness) {
            this.halfWidth = halfWidth;
            this.halfHeight = halfHeight;
            this.roundness = roundness;
        }

        boolean matches(double halfWidth, double halfHeight, double roundness) {
            return halfWidth == this.halfWidth && halfHeight == this.halfHeight && roundness == this.roundness;
        }
    }

    /** Vertices four to a quad, each a run of stride values. */
    static final class Quads {

        final int stride;
        double[] data = new double[1024];
        int size;

        Quads(int stride) {
            this.stride = stride;
        }

        /** Room for one more vertex; where it starts. */
        int grow() {
            if (size + stride > data.length) data = Arrays.copyOf(data, data.length * 2);
            size += stride;
            return size - stride;
        }

        void clear() {
            size = 0;
        }

        int vertices() {
            return size / stride;
        }

        double get(int vertex, int value) {
            return data[vertex * stride + value];
        }
    }
}
