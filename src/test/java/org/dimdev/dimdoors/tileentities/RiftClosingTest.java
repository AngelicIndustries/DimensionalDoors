package org.dimdev.dimdoors.tileentities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

public class RiftClosingTest {

    @Test
    public void swellsSlowerAndSlowerThenSnapsShut() {
        for (RiftClosing closing : RiftClosing.values()) {
            assertEquals(1, closing.scale(0), 1e-9);
            assertEquals(1 + RiftClosing.SWELL, closing.scale(closing.swell - 1e-9), 1e-6);
            assertEquals(1 + RiftClosing.SWELL, closing.scale(closing.swell), 1e-6);
            // Most of the swelling is done early, and the last tenth adds little
            double early = closing.scale(closing.swell * 0.3) - 1, late = closing.scale(closing.swell * 0.9) - 1;
            assertTrue(early > RiftClosing.SWELL * 0.6 && late > RiftClosing.SWELL * 0.95);
            assertEquals(0, closing.scale(closing.end()), 1e-9);
            assertTrue(closing.scale(closing.end() - 0.01) > 0);
            assertEquals(RiftClosing.SNAP, closing.end() - closing.swell, 1e-9);
            assertTrue(closing.endTicks() >= closing.end() * 20);
        }
    }

    @Test
    public void turnsOneWayWithoutJumps() {
        for (RiftClosing closing : RiftClosing.values()) {
            double last = 0;
            for (double t = 0; t <= closing.end() + 0.5; t += 0.01) {
                double twist = closing.twist(t);
                assertTrue(twist >= last && twist - last < 0.05);
                last = twist;
            }
        }
    }

    @Test
    public void everySoundIsPickedSomewhere() {
        Set<RiftClosing> picked = EnumSet.noneOf(RiftClosing.class);
        for (int curveId = 0; curveId < 1 << 16; curveId++) {
            picked.add(RiftClosing.of(false, curveId));
            picked.add(RiftClosing.of(true, curveId));
        }
        assertEquals(RiftClosing.values().length, picked.size());
        assertTrue(
            RiftClosing.of(true, 7)
                .isBig()
                && !RiftClosing.of(false, 7)
                    .isBig());
    }
}
