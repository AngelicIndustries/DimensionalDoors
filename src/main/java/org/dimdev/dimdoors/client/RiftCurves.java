package org.dimdev.dimdoors.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.GLUtessellator;
import org.lwjgl.util.glu.GLUtessellatorCallbackAdapter;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The shapes of rift cracks: the outlines of fractal curves drawn from L-systems, cut into triangles. From the 1.12
 * rewrite's LSystem (MIT), with LWJGL's GLU tessellator in place of poly2tri, which 1.7.10 doesn't ship.
 */
@SideOnly(Side.CLIENT)
public final class RiftCurves {

    /** A filled outline as triangles: every three points are one, in grid units. */
    public static final class Curve {

        public final int[] xs, ys;
        public final int minX, maxX, minY, maxY;

        Curve(List<int[]> points) {
            xs = new int[points.size()];
            ys = new int[points.size()];
            int loX = Integer.MAX_VALUE, hiX = Integer.MIN_VALUE, loY = Integer.MAX_VALUE, hiY = Integer.MIN_VALUE;
            for (int i = 0; i < xs.length; i++) {
                xs[i] = points.get(i)[0];
                ys[i] = points.get(i)[1];
                loX = Math.min(loX, xs[i]);
                hiX = Math.max(hiX, xs[i]);
                loY = Math.min(loY, ys[i]);
                hiY = Math.max(hiY, ys[i]);
            }
            minX = loX;
            maxX = hiX;
            minY = loY;
            maxY = hiY;
        }
    }

    // Rules ("from>to", ":" between them), the turning angle and the start. F draws a step, + and - turn.
    private static final String[] TERDRAGON = { "F>+F----F++++F-", "60", "F" };
    private static final String[] DRAGON = { "X>X+YF:Y>FX-Y", "90", "FX" };
    private static final String[] TWINDRAGON = { "X>X+YF:Y>FX-Y", "90", "FX--FX" };
    private static final String[] VORTEX = { "X>X+YF:Y>FX-Y", "90", "FX---FX" };

    private static List<Curve> curves;

    private RiftCurves() {}

    /** The curve for a rift's curve number. The first call builds them all, which takes a moment. */
    public static Curve get(int id) {
        if (curves == null) build();
        return curves.get(Math.abs(id % curves.size()));
    }

    private static synchronized void build() {
        if (curves != null) return;
        List<Curve> built = new ArrayList<Curve>();
        // The same set as 1.12; the other step counts there were left out as degenerate
        for (int steps : new int[] { 4, 5, 7 }) add(built, TERDRAGON, steps);
        for (int steps = 9; steps <= 11; steps++) add(built, VORTEX, steps);
        for (int steps = 7; steps <= 10; steps++) add(built, TWINDRAGON, steps);
        for (int steps = 8; steps <= 11; steps++) add(built, DRAGON, steps);
        curves = built;
    }

    private static void add(List<Curve> curves, String[] system, int steps) {
        Map<Character, String> rules = new HashMap<Character, String>();
        for (String rule : system[0].split(":")) {
            String[] parts = rule.split(">");
            rules.put(parts[0].charAt(0), parts[1]);
        }
        String commands = system[2];
        for (int n = 0; n < steps; n++) {
            StringBuilder next = new StringBuilder();
            for (char c : commands.toCharArray()) {
                String replaced = rules.get(c);
                next.append(replaced != null ? replaced : String.valueOf(c));
            }
            commands = next.toString();
        }
        List<int[]> triangles = tessellate(boundary(walk(Integer.parseInt(system[1]), commands, steps)));
        if (triangles.size() >= 3) curves.add(new Curve(triangles));
    }

    /** Follows the commands with a pen and returns every point it reaches, unordered. */
    private static List<double[]> walk(double angle, String commands, int steps) {
        // Turned so a terdragon points the same way whatever its step count
        int rotation = steps % 2 == 0 ? 2 : 4;
        double heading = (steps + rotation) % 4 * 90, x = 0, y = 0;
        List<double[]> points = new ArrayList<double[]>();
        ArrayDeque<double[]> saved = new ArrayDeque<double[]>();
        for (char c : commands.toCharArray()) {
            if (c == 'F') {
                x -= Math.cos(Math.toRadians(heading));
                y -= Math.sin(Math.toRadians(heading));
                points.add(new double[] { x, y });
            } else if (c == '-') heading = (heading - angle) % 360;
            else if (c == '+') heading = (heading + angle) % 360;
            else if (c == '[') saved.push(new double[] { heading, x, y });
            else if (c == ']') {
                double[] state = saved.pop();
                heading = state[0];
                x = state[1];
                y = state[2];
            }
        }
        return points;
    }

    /**
     * Walks around the outside of the curve's points on the grid, always turning to the next outside point, and
     * returns the closed outline in order, without zero-width spikes.
     */
    private static List<int[]> boundary(List<double[]> input) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        Set<Long> cells = new HashSet<Long>();
        for (double[] point : input) {
            int x = (int) Math.round(point[0]), y = (int) Math.round(point[1]);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            cells.add(key(x, y));
        }
        int[] start = { minX, minY };
        int[] previous = start.clone();
        while (start[1] < maxY && !cells.contains(key(start[0], start[1]))) start[1]++;
        final int[] first = start.clone();
        int[] direction = direction(previous, start);
        List<int[]> outline = new ArrayList<int[]>();
        do {
            // Turn a quarter counter-clockwise and look one step that way
            direction = new int[] { -direction[1], direction[0] };
            int[] target = { start[0] + direction[0], start[1] + direction[1] };
            if (cells.contains(key(target[0], target[1]))) {
                if (same(target, first)) {
                    outline.remove(outline.size() - 1);
                    break;
                }
                direction = direction(start, target);
                if (outline.size() > 1 && same(outline.get(outline.size() - 2), target)) {
                    outline.remove(outline.size() - 1);
                } else {
                    int index = indexOf(outline, target);
                    if (index >= 0 && index != 0) while (outline.size() > index) outline.remove(outline.size() - 1);
                    outline.add(target);
                }
                start = target;
            }
        } while (!(outline.size() > 1 && same(outline.get(outline.size() - 1), first))
            && outline.size() < cells.size());
        return outline;
    }

    private static long key(int x, int y) {
        return (long) x << 32 | y & 0xFFFFFFFFL;
    }

    private static boolean same(int[] a, int[] b) {
        return a[0] == b[0] && a[1] == b[1];
    }

    private static int indexOf(List<int[]> points, int[] point) {
        for (int i = 0; i < points.size(); i++) if (same(points.get(i), point)) return i;
        return -1;
    }

    /** The step from destination back toward origin, as -1, 0 or 1 on each axis. */
    private static int[] direction(int[] origin, int[] destination) {
        return new int[] { Integer.signum(origin[0] - destination[0]), Integer.signum(origin[1] - destination[1]) };
    }

    /** Cuts the outline into triangles; asking for edge flags makes GLU return plain triangles only. */
    private static List<int[]> tessellate(List<int[]> outline) {
        final List<int[]> triangles = new ArrayList<int[]>();
        GLUtessellator tessellator = GLU.gluNewTess();
        GLUtessellatorCallbackAdapter callback = new GLUtessellatorCallbackAdapter() {

            @Override
            public void vertex(Object data) {
                double[] point = (double[]) data;
                triangles.add(new int[] { (int) point[0], (int) point[1] });
            }

            @Override
            public void edgeFlag(boolean boundaryEdge) {}

            @Override
            public void combine(double[] coords, Object[] data, float[] weight, Object[] out) {
                out[0] = new double[] { coords[0], coords[1], coords[2] };
            }
        };
        tessellator.gluTessCallback(GLU.GLU_TESS_VERTEX, callback);
        tessellator.gluTessCallback(GLU.GLU_TESS_EDGE_FLAG, callback);
        tessellator.gluTessCallback(GLU.GLU_TESS_COMBINE, callback);
        tessellator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD);
        tessellator.gluTessBeginPolygon(null);
        tessellator.gluTessBeginContour();
        for (int[] point : outline) {
            double[] coords = { point[0], point[1], 0 };
            tessellator.gluTessVertex(coords, 0, coords);
        }
        tessellator.gluTessEndContour();
        tessellator.gluTessEndPolygon();
        tessellator.gluDeleteTess();
        // A triangle cut short by an error would shift every later one
        while (triangles.size() % 3 != 0) triangles.remove(triangles.size() - 1);
        return triangles;
    }
}
