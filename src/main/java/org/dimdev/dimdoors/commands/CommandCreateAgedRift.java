package org.dimdev.dimdoors.commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.core.LinkType;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.tileentities.TileEntityRift;

/**
 * /dd-rift: with an age (like 90s, 15m, 1h30m; a bare number is minutes), a debug rift a few blocks in front of the
 * player, grown as if it had been loaded that long, leading to a random dungeon. With anything else, the original
 * command: a rift to the dungeon of that name (CommandCreateDungeonRift, which this one takes the name from).
 */
public class CommandCreateAgedRift extends DDCommandBase {

    private static final Pattern PART = Pattern.compile("(\\d+(?:\\.\\d+)?)([smh]?)");
    private static CommandCreateAgedRift instance = null;

    private CommandCreateAgedRift() {
        super("dd-rift", "<dungeon name, or an age like 90s, 15m or 1h30m>");
    }

    public static CommandCreateAgedRift instance() {
        if (instance == null) instance = new CommandCreateAgedRift();
        return instance;
    }

    @Override
    protected DDCommandResult processCommand(EntityPlayer sender, String[] command) {
        if (command.length < 1) return DDCommandResult.TOO_FEW_ARGUMENTS;
        long ticks = command.length == 1 ? ticks(command[0]) : -1;
        if (ticks < 0) return CommandCreateDungeonRift.instance()
            .processCommand(sender, command);

        Vec3 look = sender.getLookVec();
        int x = MathHelper.floor_double(sender.posX + look.xCoord * 3);
        int y = MathHelper.floor_double(sender.posY + sender.getEyeHeight() + look.yCoord * 3);
        int z = MathHelper.floor_double(sender.posZ + look.zCoord * 3);
        if (!sender.worldObj.isAirBlock(x, y, z))
            return new DDCommandResult("Error: The spot in front of you isn't air.");
        int orientation = MathHelper.floor_double((sender.rotationYaw + 180.0F) * 4.0F / 360.0F - 0.5D) & 3;
        PocketManager.getDimensionData(sender.worldObj)
            .createLink(x, y, z, LinkType.DUNGEON, orientation);
        sender.worldObj.setBlock(x, y, z, DimDoors.blockRift, 0, 3);
        TileEntity tile = sender.worldObj.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityRift)) return new DDCommandResult("Error: The rift has no tile entity.");
        TileEntityRift rift = (TileEntityRift) tile;
        rift.size = TileEntityRift.sizeAfter(ticks, DimDoors.properties);
        rift.markDirty();
        sender.worldObj.markBlockForUpdate(x, y, z);
        sendChat(
            sender,
            String.format(
                "Created a rift aged %s: size %.0f, unravel radius %.2f (sounds big from %.1f).",
                command[0],
                rift.size,
                rift.radius(),
                TileEntityRift.BIG_RADIUS));
        return DDCommandResult.SUCCESS;
    }

    /** The age in ticks, or -1 if it doesn't read as one. */
    static long ticks(String age) {
        Matcher matcher = PART.matcher(age.toLowerCase());
        double seconds = 0;
        int end = 0;
        while (matcher.find() && matcher.start() == end) {
            double value = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2);
            seconds += value * ("s".equals(unit) ? 1 : "h".equals(unit) ? 3600 : 60);
            end = matcher.end();
        }
        return end == age.length() && end > 0 ? Math.round(seconds * 20) : -1;
    }
}
