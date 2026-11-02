package org.dimdev.dimdoors.commands;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import org.dimdev.dimdoors.core.DimensionType;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.pockets.Pocket;
import org.dimdev.dimdoors.pockets.PocketRegistry;
import org.dimdev.dimdoors.pockets.SharedPockets;

public class CommandResetDungeons extends DDCommandBase {

    private static CommandResetDungeons instance = null;

    private CommandResetDungeons() {
        super("dd-resetdungeons", "");
    }

    public static CommandResetDungeons instance() {
        if (instance == null) instance = new CommandResetDungeons();

        return instance;
    }

    @Override
    protected DDCommandResult processCommand(EntityPlayer sender, String[] command) {
        if (command.length > 0) {
            return DDCommandResult.TOO_MANY_ARGUMENTS;
        }

        // A dungeon's region file can't be deleted while its dimension may be loaded. Mark the dungeons instead: they
        // are reset at the next server start, before the dimension loads. Dungeons with players in them are skipped.
        PocketRegistry dungeons = SharedPockets.registry(DimensionType.DUNGEON);
        World world = DimensionManager.getWorld(dungeons.dimension());
        int marked = 0;
        for (Pocket dungeon : dungeons.pockets()) {
            if (world == null || !hasPlayers(world, dungeon)) {
                dungeons.markForReset(dungeon);
                marked++;
            }
        }
        PocketManager.save(true);

        // Notify the user of the results
        sendChat(
            sender,
            (marked + " out of "
                + dungeons.pockets()
                    .size()
                + " dungeons will be reset when the server restarts."));
        return DDCommandResult.SUCCESS;
    }

    private static boolean hasPlayers(World world, Pocket dungeon) {
        for (Object entity : world.playerEntities) {
            EntityPlayer player = (EntityPlayer) entity;
            if (dungeon.contains(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posZ))) {
                return true;
            }
        }
        return false;
    }
}
