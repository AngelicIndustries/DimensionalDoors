package org.dimdev.dimdoors.client;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.ResourceLocation;

/**
 * A sound at the player's ears whose volume eases toward a target, by a full fade over fadeTicks. Done once it has
 * faded out after stop(), or when a sound that doesn't loop ends.
 */
public class FadingSound extends MovingSound {

    private final float step;
    private float target;
    private boolean stopping;

    public FadingSound(ResourceLocation sound, boolean loop, float target, int fadeTicks) {
        super(sound);
        this.repeat = loop;
        this.field_147665_h = 0;
        this.field_147666_i = ISound.AttenuationType.NONE;
        this.target = target;
        this.step = fadeTicks > 0 ? 1.0F / fadeTicks : 1.0F;
        // A sound that starts at zero volume isn't played at all
        this.volume = fadeTicks > 0 ? Math.min(target, 0.01F) : target;
    }

    public void setTarget(float target) {
        if (!stopping) this.target = target;
    }

    public void stop() {
        stopping = true;
        target = 0;
    }

    public boolean isStopping() {
        return stopping;
    }

    @Override
    public void update() {
        if (volume < target) volume = Math.min(target, volume + step);
        else if (volume > target) volume = Math.max(target, volume - step);
        if (stopping && volume <= 0) donePlaying = true;
    }
}
