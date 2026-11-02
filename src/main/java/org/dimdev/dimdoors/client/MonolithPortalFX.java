package org.dimdev.dimdoors.client;

import net.minecraft.client.particle.EntityPortalFX;
import net.minecraft.world.World;

/** Vanilla's portal particle in red, drawn around a player a Monolith is about to send to Limbo. */
public class MonolithPortalFX extends EntityPortalFX {

    public MonolithPortalFX(World world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z, motionX, motionY, motionZ);
        // Vanilla tints its random brightness purple; blue keeps it as is
        float brightness = this.particleBlue;
        this.particleRed = brightness;
        this.particleGreen = brightness * 0.1F;
        this.particleBlue = brightness * 0.1F;
    }
}
