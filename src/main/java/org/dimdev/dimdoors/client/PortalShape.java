package org.dimdev.dimdoors.client;

import java.util.Arrays;
import java.util.Random;

/**
 * The shape of a door's portal for one frame, in the doorway's plane: h across and v up from the doorway's middle. An
 * oval whose edge is a fractal (bulges on bulges, each row twice as many and about two thirds as tall) turning slowly
 * around it, shaken a little, with a glowing rim whose outer edge flickers in spikes turning the other way. Now and
 * then a glitch cuts it into bands and shifts them sideways. All of it is quads, as shader packs expect.
 */
final class PortalShape {

    /** Per fill vertex: h and v of its spot on the oval (where its texture is), and h and v where it is drawn. */
    static final int FILL_STRIDE = 4;
    /** Per rim vertex: the same, then its glow (0 to 1), the turn around the oval (0 to 1) and how far out (0 to 1). */
    static final int RIM_STRIDE = 7;

    private static final int POINTS = 384;
    /** The oval's half width and height, as parts of the doorway's. */
    private static final double WIDTH = 0.8, HEIGHT = 0.88;
    /** The edge's rows of bulges: how many fit around the oval, and how far each moves the edge, in blocks. */
    private static final int[] WAVES = { 8, 16, 32, 64 };
    private static final double[] HEIGHTS = { 0.022, 0.015, 0.01, 0.0065 };
    /** How fast the edge turns around the oval, and how fast each row drifts on its own, in radians a second. */
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
    private final double[] fan = new double[2 * (POINTS + 2)], rimIn = new double[20], rimOut = new double[20];
    private final double[] edgePhase = new double[WAVES.length], flamePhase = new double[WAVES.length];

    /** Made into the oval at a time, inside a doorway of the given half size, its own per seed. */
    void oval(long seed, long millis, double halfWidth, double halfHeight) {
        fill.clear();
        rim.clear();
        flash = 0;
        double time = millis / 1000.0;
        Random shake = new Random(mix(seed, millis / SHAKE_MS));
        double ch = (shake.nextDouble() * 2 - 1) * SHAKE, cv = (shake.nextDouble() * 2 - 1) * SHAKE;
        double pulse = (shake.nextDouble() * 2 - 1) * PULSE;
        Random phases = new Random(seed);
        for (int k = 0; k < WAVES.length; k++) {
            edgePhase[k] = phases.nextDouble() * Math.PI * 2;
            flamePhase[k] = phases.nextDouble() * Math.PI * 2;
        }

        double a = halfWidth * WIDTH, b = halfHeight * HEIGHT;
        for (int i = 0; i <= POINTS; i++) {
            double angle = Math.PI * 2 * (i % POINTS) / POINTS;
            double ph = a * Math.cos(angle), pv = b * Math.sin(angle);
            double length = Math.sqrt(ph * ph + pv * pv);
            double uh = ph / length, uv = pv / length;
            double reach = reach(ch, cv, uh, uv, halfWidth, halfHeight);
            double r = length + pulse + waves(angle, time, SPIN, edgePhase, false);
            double flame = 0.5 + 0.5 * waves(angle, time, FLAME_SPIN, flamePhase, true) / HEIGHTS[0];
            double out = r + RIM_OUT * (0.4 + 0.6 * Math.max(0, Math.min(1, flame)));
            put(edge, i, ch, cv, uh, uv, Math.min(r, reach));
            put(inner, i, ch, cv, uh, uv, Math.min(r - RIM_IN, reach));
            put(outer, i, ch, cv, uh, uv, Math.min(out, reach));
        }

        double[][] bands = glitchBands(seed, millis, halfHeight);
        if (bands == null) {
            // Unbroken: the fill is one fan around the middle
            fan[0] = ch;
            fan[1] = cv;
            System.arraycopy(edge, 0, fan, 2, edge.length);
            addFan(fill, fan, 2, POINTS + 2, 0, ch, cv);
            for (int i = 0; i < POINTS; i++) addRimQuads(i, null, 0, ch, cv);
            return;
        }
        for (double[] band : bands) {
            double[] bounds = { band[0], band[1], -halfWidth - band[2], halfWidth - band[2] };
            for (int i = 0; i < POINTS; i++) {
                double[] triangle = { ch, cv, edge[2 * i], edge[2 * i + 1], edge[2 * i + 2], edge[2 * i + 3] };
                double[] piece = clip(triangle, 2, bounds);
                if (piece != null) addFan(fill, piece, 2, piece.length / 2, band[2], ch, cv);
                addRimQuads(i, bounds, band[2], ch, cv);
            }
        }
    }

    /**
     * The fractal at an angle around the oval, turning at the spin: rows of round bulges meeting in sharp dips, or
     * turned over, sharp spikes. Averages 0.
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

    /** How far from the middle along a direction the doorway reaches. */
    private static double reach(double ch, double cv, double uh, double uv, double halfWidth, double halfHeight) {
        double reach = Double.MAX_VALUE;
        if (uh > 0) reach = (halfWidth - ch) / uh;
        else if (uh < 0) reach = (-halfWidth - ch) / uh;
        if (uv > 0) reach = Math.min(reach, (halfHeight - cv) / uv);
        else if (uv < 0) reach = Math.min(reach, (-halfHeight - cv) / uv);
        return reach;
    }

    private static void put(double[] ring, int i, double ch, double cv, double uh, double uv, double r) {
        ring[2 * i] = ch + uh * r;
        ring[2 * i + 1] = cv + uv * r;
    }

    /**
     * The bands a glitch cuts the portal into right now, each {v from, v to, h shift}, or null when it isn't
     * glitching. Sets the flash.
     */
    private double[][] glitchBands(long seed, long millis, double halfHeight) {
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
        at[0] = -halfHeight * 2;
        at[1] = halfHeight * 2;
        for (int i = 2; i < at.length; i++) at[i] = (random.nextDouble() * 2 - 1) * halfHeight;
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
