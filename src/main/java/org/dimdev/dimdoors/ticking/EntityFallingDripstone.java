package org.dimdev.dimdoors.ticking;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.dimdev.dimdoors.mixins.AccessorEntityFallingBlock;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;

/**
 * A stalactite of Et Futurum's pointed dripstone that lost its hold, falling as in 6.x: the tip hurts what it lands on
 * and every piece breaks into an item where it lands.
 */
public class EntityFallingDripstone extends EntityFallingBlock implements IEntityAdditionalSpawnData {

    /** 6.x's falling tip: six per block fallen, at most 40. Et Futurum names the death message. */
    private static final int DAMAGE_PER_BLOCK = 6, MAX_DAMAGE = 40;
    private static final DamageSource STALACTITE = new DamageSource("stalactite");

    private boolean tip;

    public EntityFallingDripstone(World world) {
        super(world);
        ((AccessorEntityFallingBlock) this).dimdoors$setVanishes(true);
    }

    public EntityFallingDripstone(World world, int x, int y, int z, Block block, int meta, boolean tip) {
        super(world, x + 0.5, y + 0.5, z + 0.5, block, meta);
        this.tip = tip;
        // The block is gone already: skip the first tick's check that it's still there
        field_145812_b = 1;
        ((AccessorEntityFallingBlock) this).dimdoors$setVanishes(true);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!worldObj.isRemote && isDead && onGround) {
            Block block = func_145805_f();
            worldObj.playSoundEffect(posX, posY, posZ, block.stepSound.getBreakSound(), 1.0F, 0.9F);
            entityDropItem(new ItemStack(block, 1, block.damageDropped(field_145814_a)), 0.0F);
        }
    }

    @Override
    protected void fall(float distance) {
        int blocks = MathHelper.ceiling_float_int(distance - 1.0F);
        if (!tip || blocks <= 0) return;
        for (Object entity : new ArrayList<Object>(worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox))) {
            ((Entity) entity).attackEntityFrom(STALACTITE, Math.min(blocks * DAMAGE_PER_BLOCK, MAX_DAMAGE));
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean("Tip", tip);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        tip = tag.getBoolean("Tip");
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeInt(Block.getIdFromBlock(func_145805_f()));
        buffer.writeByte(field_145814_a);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        ((AccessorEntityFallingBlock) this).dimdoors$setBlock(Block.getBlockById(buffer.readInt()));
        field_145814_a = buffer.readByte();
    }
}
