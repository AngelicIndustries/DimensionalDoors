package org.dimdev.dimdoors.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The in-game config screen, a page per category of DimDoors.cfg. Most settings take effect when it's closed; the
 * ones marked as needing a restart (IDs, recipes, loot tables, door variants) wait for the next start.
 */
public class GuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {}

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ConfigScreen.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class ConfigScreen extends GuiConfig {

        public ConfigScreen(GuiScreen parentScreen) {
            super(parentScreen, pages(), DimDoors.modid, false, false, "Dimensional Doors");
        }

        @SuppressWarnings("rawtypes")
        private static List<IConfigElement> pages() {
            DDProperties properties = DDProperties.instance();
            List<IConfigElement> pages = new ArrayList<>();
            for (String category : properties.screenCategories()) {
                pages.add(new ConfigElement<ConfigCategory>(properties.Config.getCategory(category)));
            }
            return pages;
        }
    }

    /** Applies the screen's changes once it's closed. */
    public static class Changes {

        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (DimDoors.modid.equals(event.modID)) DDProperties.instance()
                .reload();
        }
    }
}
