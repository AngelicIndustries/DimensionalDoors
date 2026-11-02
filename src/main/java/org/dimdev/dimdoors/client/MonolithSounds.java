package org.dimdev.dimdoors.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.ticking.MobMonolith;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import industries.angelic.angellib.util.sound.ICustomMaxDistanceSound;
import industries.angelic.angellib.util.sound.ICustomSourceRadiusSound;

/**
 * The Monoliths as the player hears them, played on the client from what it knows of them, each from where its
 * Monolith stands. A Monolith that hasn't noticed the player drones (the nearest few within hearing). One that notices
 * the player, by coming within its aggro range, lets out a sub rush. While one can see the player and its red particles
 * gather, a bass sweep loops, after a surge one time in three, and fades out slowly once it can't.
 */
public final class MonolithSounds {

    private static final ResourceLocation DRONE = new ResourceLocation(DimDoors.modid, "monolith.drone");
    private static final ResourceLocation NOTICE = new ResourceLocation(DimDoors.modid, "monolith.notice");
    private static final ResourceLocation AGGRO = new ResourceLocation(DimDoors.modid, "monolith.aggro");
    private static final ResourceLocation SURGE = new ResourceLocation(DimDoors.modid, "monolith.surge");

    /**
     * How far each is heard. Vanilla fades a sound out linearly to there; Sound Physics plays it at full volume up to a
     * sixteenth of it (the aggro sounds up to 32 blocks, the notice up to 24) and then rolls it off.
     */
    private static final float DRONE_RANGE = 64, NOTICE_RANGE = 384, AGGRO_RANGE = 512;
    /** Only the Monoliths this near drone. */
    private static final double DRONE_DISTANCE = 32;
    /** Each is heard as coming from a body this wide (a Monolith is 3 blocks across), with AngelLib. */
    private static final float SOURCE_RADIUS = 1.5F;
    /** AngelLib sets a sound's range apart from its volume; without it, its starting volume sets it. */
    private static final boolean ANGELLIB = Loader.isModLoaded("angellib");
    /** How far past its aggro range a player must be before a Monolith drones again, so it doesn't pop back in. */
    private static final double DRONE_RETURN = 4;
    private static final int MAX_DRONES = 3;
    private static final int SURGE_CHANCE = 3;
    /** Dirty Bass FX's length, after which the aggro loop starts. */
    private static final int SURGE_TICKS = 125;
    /** The aggro loop starts at full volume (An Extra Touch's camera shake goes by it) and fades out slowly. */
    private static final int DRONE_FADE_TICKS = 20, AGGRO_FADE_OUT_TICKS = 80;

    private final Map<Integer, Follower> drones = new HashMap<>();
    private final Set<Integer> noticed = new HashSet<>();
    private final Random random = new Random();
    private World world;
    private Follower aggroLoop;
    /** Ticks until the aggro loop starts, or -1 when none is waiting. */
    private int loopDelay = -1;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != world) {
            stopAll();
            world = mc.theWorld;
        }
        if (world == null || mc.thePlayer == null || mc.isGamePaused()) return;
        EntityPlayer player = mc.thePlayer;
        double range = DDProperties.instance().MonolithAggroRange;
        List<MobMonolith> quiet = new ArrayList<>();
        Set<Integer> present = new HashSet<>();
        MobMonolith seenBy = null;
        for (Object entity : world.loadedEntityList) {
            if (!(entity instanceof MobMonolith)) continue;
            MobMonolith monolith = (MobMonolith) entity;
            int id = monolith.getEntityId();
            present.add(id);
            double distance = monolith.getDistanceToEntity(player);
            if (monolith.isDangerous() && distance <= range) {
                if (noticed.add(id)) mc.getSoundHandler()
                    .playSound(new Follower(NOTICE, monolith, false, NOTICE_RANGE, 1, 1));
                // As the Monolith decides to spawn its particles; the nearest one that sees the player is heard
                if (player.canEntityBeSeen(monolith)
                    && (seenBy == null || distance < seenBy.getDistanceToEntity(player))) seenBy = monolith;
                continue;
            }
            if (noticed.contains(id) && distance <= range + DRONE_RETURN) continue;
            noticed.remove(id);
            if (distance <= DRONE_DISTANCE) quiet.add(monolith);
        }
        noticed.retainAll(present);
        updateDrones(mc, quiet, player);
        updateAggro(mc, seenBy);
    }

    /** Drones for the nearest quiet Monoliths; the others fade out. */
    private void updateDrones(Minecraft mc, List<MobMonolith> quiet, EntityPlayer player) {
        quiet.sort(Comparator.comparingDouble(monolith -> monolith.getDistanceSqToEntity(player)));
        Set<Integer> keep = new HashSet<>();
        for (MobMonolith monolith : quiet.subList(0, Math.min(MAX_DRONES, quiet.size()))) {
            keep.add(monolith.getEntityId());
            Follower drone = drones.get(monolith.getEntityId());
            if (drone == null || drone.isDonePlaying()) {
                drone = new Follower(DRONE, monolith, true, DRONE_RANGE, 1, DRONE_FADE_TICKS);
                drones.put(monolith.getEntityId(), drone);
                mc.getSoundHandler()
                    .playSound(drone);
            }
        }
        for (Iterator<Map.Entry<Integer, Follower>> it = drones.entrySet()
            .iterator(); it.hasNext();) {
            Map.Entry<Integer, Follower> entry = it.next();
            if (!keep.contains(entry.getKey())) entry.getValue()
                .stop();
            if (entry.getValue()
                .isDonePlaying()) it.remove();
        }
    }

    /**
     * The bass sweep loop from the Monolith that sees the player, sometimes after a surge. Once none does it fades out
     * slowly, and comes back up if one sees the player again before it has.
     */
    private void updateAggro(Minecraft mc, MobMonolith seenBy) {
        if (aggroLoop != null && aggroLoop.isDonePlaying()) aggroLoop = null;
        if (seenBy == null) {
            if (aggroLoop != null) aggroLoop.setTarget(0);
            loopDelay = -1;
            return;
        }
        if (aggroLoop != null) {
            aggroLoop.setEntity(seenBy);
            aggroLoop.setTarget(1);
            return;
        }
        if (loopDelay < 0) {
            // A new aggro
            loopDelay = 0;
            if (random.nextInt(SURGE_CHANCE) == 0) {
                mc.getSoundHandler()
                    .playSound(new Follower(SURGE, seenBy, false, AGGRO_RANGE, 1, 1));
                loopDelay = SURGE_TICKS;
            }
        }
        if (loopDelay > 0) {
            loopDelay--;
            return;
        }
        loopDelay = -1;
        aggroLoop = new Follower(AGGRO, seenBy, true, AGGRO_RANGE, 1, AGGRO_FADE_OUT_TICKS);
        mc.getSoundHandler()
            .playSound(aggroLoop);
    }

    private void stopAll() {
        for (Follower drone : drones.values()) drone.stop();
        drones.clear();
        noticed.clear();
        if (aggroLoop != null) aggroLoop.stop();
        aggroLoop = null;
        loopDelay = -1;
    }

    /**
     * A sound that follows a Monolith, heard up to its range and so from its direction, with Sound Physics' reverb, and
     * faded toward a target volume. With AngelLib its range and width are its own; without, its range comes from
     * starting at full volume (16 blocks per unit), which a fade in would cut short.
     */
    @Optional.InterfaceList({
        @Optional.Interface(
            iface = "industries.angelic.angellib.util.sound.ICustomMaxDistanceSound",
            modid = "angellib"),
        @Optional.Interface(
            iface = "industries.angelic.angellib.util.sound.ICustomSourceRadiusSound",
            modid = "angellib") })
    private static final class Follower extends MovingSound
        implements ICustomMaxDistanceSound, ICustomSourceRadiusSound {

        private Entity entity;
        private final float range, fadeIn, fadeOut;
        private float gain, target = 1;
        private boolean stopping;

        Follower(ResourceLocation sound, Entity entity, boolean loop, float range, int fadeInTicks, int fadeOutTicks) {
            super(sound);
            this.entity = entity;
            this.range = range;
            this.repeat = loop;
            this.field_147665_h = 0;
            this.fadeIn = 1.0F / fadeInTicks;
            this.fadeOut = 1.0F / fadeOutTicks;
            // A sound that starts at zero volume isn't played at all
            this.gain = ANGELLIB && fadeInTicks > 1 ? 0.01F : 1;
            this.volume = ANGELLIB ? gain : range / 16;
            follow();
        }

        @Override
        @Optional.Method(modid = "angellib")
        public float getMaxSoundDistance() {
            return range;
        }

        @Override
        @Optional.Method(modid = "angellib")
        public float getSoundSourceRadius() {
            return SOURCE_RADIUS;
        }

        void setEntity(Entity entity) {
            this.entity = entity;
        }

        void setTarget(float target) {
            if (!stopping) this.target = target;
        }

        void stop() {
            stopping = true;
            target = 0;
        }

        private void follow() {
            this.xPosF = (float) entity.posX;
            this.yPosF = (float) (entity.posY + entity.getEyeHeight());
            this.zPosF = (float) entity.posZ;
        }

        @Override
        public void update() {
            if (entity.isDead || entity.worldObj != Minecraft.getMinecraft().theWorld) stop();
            follow();
            if (gain < target) gain = Math.min(target, gain + fadeIn);
            else if (gain > target) gain = Math.max(target, gain - fadeOut);
            this.volume = gain;
            if (target <= 0 && gain <= 0) this.donePlaying = true;
        }
    }
}
