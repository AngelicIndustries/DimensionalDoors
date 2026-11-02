package org.dimdev.dimdoors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;
import net.minecraftforge.event.world.WorldEvent;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ClientOnlyHooks {

    private final DDProperties properties;

    private final ISound limboMusic;

    public ClientOnlyHooks(DDProperties properties) {
        this.properties = properties;
        this.limboMusic = PositionedSoundRecord.func_147673_a(new ResourceLocation(DimDoors.modid + ":creepy"));
    }

    /**
     * In Limbo, Limbo's music takes the place of the game's: vanilla's music.game plays it instead (when it isn't
     * already
     * playing), and every other music, vanilla or another mod's, is kept silent there.
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onSoundEffectResult(PlaySoundEvent17 event) {
        ResourceLocation playingSound = event.sound == null ? null : event.sound.getPositionedSoundLocation();
        Minecraft mc = Minecraft.getMinecraft();
        if (playingSound == null || mc.theWorld == null
            || mc.theWorld.provider.dimensionId != DimDoors.properties.LimboDimensionID
            || playingSound.getResourceDomain()
                .equals(DimDoors.modid)) {
            return;
        }
        if (playingSound.getResourceDomain()
            .equals("minecraft")
            && (playingSound.getResourcePath()
                .equals("music.game")
                || playingSound.getResourcePath()
                    .equals("music.game.creative"))) {
            // A null result plays nothing (Event.Result.DENY isn't heeded for sounds)
            event.result = mc.getSoundHandler()
                .isSoundPlaying(limboMusic) ? null : limboMusic;
            return;
        }
        SoundEventAccessorComposite sound = mc.getSoundHandler()
            .getSound(playingSound);
        if (sound != null && sound.getSoundCategory() == SoundCategory.MUSIC) event.result = null;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world.provider.dimensionId == DimDoors.properties.LimboDimensionID && event.world.isRemote
            && !Minecraft.getMinecraft()
                .getSoundHandler()
                .isSoundPlaying(limboMusic)) {
            Minecraft.getMinecraft()
                .getSoundHandler()
                .playSound(limboMusic);
        }
    }
}
