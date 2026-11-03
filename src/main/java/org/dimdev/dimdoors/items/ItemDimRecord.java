package org.dimdev.dimdoors.items;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemRecord;
import net.minecraft.util.ResourceLocation;

import org.dimdev.dimdoors.DimDoors;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** One of 6.x's music discs: plays records.song from our sounds, shows its texture as record_song. */
public class ItemDimRecord extends ItemRecord {

    private final String song;

    public ItemDimRecord(String song) {
        super(DimDoors.modid + "." + song);
        this.song = song;
        setCreativeTab(DimDoors.dimDoorsCreativeTab);
        setUnlocalizedName("record");
        setTextureName(DimDoors.modid + ":record_" + song);
    }

    @Override
    public ResourceLocation getRecordResource(String name) {
        return new ResourceLocation(DimDoors.modid, "records." + song);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister iconRegister) {
        itemIcon = iconRegister.registerIcon(getIconString());
    }
}
