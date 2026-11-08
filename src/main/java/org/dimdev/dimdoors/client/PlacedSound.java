package org.dimdev.dimdoors.client;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import industries.angelic.angellib.util.sound.ICustomMaxDistanceSound;

/**
 * A looping sound at a spot, heard up to its range and faded toward a target volume. With AngelLib its range is its
 * own; without, it comes from starting at full volume (16 blocks per unit) and the fade only goes down from there.
 */
@Optional.Interface(iface = "industries.angelic.angellib.util.sound.ICustomMaxDistanceSound", modid = "angellib")
public class PlacedSound extends MovingSound implements ICustomMaxDistanceSound {

    private static final boolean ANGELLIB = Loader.isModLoaded("angellib");

    private final float range, step;
    private float gain, target;
    private boolean stopping;

    public PlacedSound(ResourceLocation sound, double x, double y, double z, float range, float target, int fadeTicks) {
        super(sound);
        this.repeat = true;
        this.field_147665_h = 0;
        this.range = range;
        this.target = target;
        this.step = 1.0F / Math.max(1, fadeTicks);
        // A sound that starts at zero volume isn't played at all
        this.gain = ANGELLIB ? Math.min(target, 0.01F) : target;
        this.volume = ANGELLIB ? gain : gain * range / 16;
        moveTo(x, y, z);
    }

    @Override
    @Optional.Method(modid = "angellib")
    public float getMaxSoundDistance() {
        return range;
    }

    public void moveTo(double x, double y, double z) {
        this.xPosF = (float) x;
        this.yPosF = (float) y;
        this.zPosF = (float) z;
    }

    public void setTarget(float target) {
        if (!stopping) this.target = target;
    }

    public void stop() {
        stopping = true;
        target = 0;
    }

    @Override
    public void update() {
        if (gain < target) gain = Math.min(target, gain + step);
        else if (gain > target) gain = Math.max(target, gain - step);
        this.volume = ANGELLIB ? gain : gain * range / 16;
        if (stopping && gain <= 0) this.donePlaying = true;
    }
}
