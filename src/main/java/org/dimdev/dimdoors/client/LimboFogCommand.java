package org.dimdev.dimdoors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** /limbofog: a slider for Limbo's fog, to tune it by eye (GuiLimboFog). */
@SideOnly(Side.CLIENT)
public final class LimboFogCommand extends CommandBase {

    /** Opens on the next tick, once the chat that ran the command has closed. */
    private static boolean pending;

    private LimboFogCommand() {}

    public static void register() {
        ClientCommandHandler.instance.registerCommand(new LimboFogCommand());
        FMLCommonHandler.instance()
            .bus()
            .register(new Opener());
    }

    @Override
    public String getCommandName() {
        return "limbofog";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/limbofog";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        pending = true;
    }

    public static final class Opener {

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !pending) return;
            pending = false;
            Minecraft.getMinecraft()
                .displayGuiScreen(new GuiLimboFog());
        }
    }
}
