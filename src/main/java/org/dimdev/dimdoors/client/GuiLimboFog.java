package org.dimdev.dimdoors.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.Configuration;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;

import cpw.mods.fml.client.config.GuiSlider;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A slider for Limbo's fog (/limbofog), with Limbo in view: the fog follows the slider at once. Without shaders it sets
 * the density; under a shader pack, that pack's own strength, with a second slider for its light. Apply saves them to
 * DimDoors.cfg; leaving without it goes back to the saved values. Reset picks the default.
 */
@SideOnly(Side.CLIENT)
public class GuiLimboFog extends GuiScreen implements GuiSlider.ISlider {

    private static final int SLIDER = 0, RESET = 1, APPLY = 2, DONE = 3, LIGHT = 4, LIGHT_RESET = 5;
    private static final int LEFT = 8, WIDTH = 220, RESET_WIDTH = 40;
    private static final double MAX = 0.5, DEFAULT = 0.1, SHADER_MAX = 2, LIGHT_MAX = 1;
    private static final String KEY = "Limbo Fog Density";

    private final boolean shaders = ShaderPacks.inUse();
    private final String pack = shaders ? ShaderPacks.name() : null;
    private GuiSlider slider, light;
    private double saved = LimboFog.density();
    private String status = shaders ? I18n.format("dimdoors.limbofog.shaders.hint") : "";

    @Override
    public void initGui() {
        buttonList.clear();
        double value = shaders ? LimboFog.shaderStrength() : LimboFog.density();
        slider = new GuiSlider(
            SLIDER,
            LEFT,
            24,
            WIDTH - RESET_WIDTH - 4,
            20,
            "",
            "",
            0,
            shaders ? SHADER_MAX : MAX,
            value,
            true,
            false,
            this);
        slider.precision = shaders ? 2 : 3;
        slider.displayString = label(value);
        buttonList.add(slider);
        buttonList.add(
            new GuiButton(
                RESET,
                LEFT + WIDTH - RESET_WIDTH,
                24,
                RESET_WIDTH,
                20,
                I18n.format("dimdoors.limbofog.reset")));
        int y = 50;
        if (shaders) {
            light = new GuiSlider(
                LIGHT,
                LEFT,
                48,
                WIDTH - RESET_WIDTH - 4,
                20,
                "",
                "",
                0,
                LIGHT_MAX,
                LimboFog.shaderLight(),
                true,
                false,
                this);
            light.precision = 2;
            light.displayString = lightLabel(LimboFog.shaderLight());
            buttonList.add(light);
            buttonList.add(
                new GuiButton(
                    LIGHT_RESET,
                    LEFT + WIDTH - RESET_WIDTH,
                    48,
                    RESET_WIDTH,
                    20,
                    I18n.format("dimdoors.limbofog.reset")));
            y = 74;
        }
        buttonList.add(new GuiButton(APPLY, LEFT, y, WIDTH / 2 - 2, 20, I18n.format("dimdoors.limbofog.apply")));
        buttonList.add(new GuiButton(DONE, LEFT + WIDTH / 2 + 2, y, WIDTH / 2 - 2, 20, I18n.format("gui.done")));
    }

    @Override
    public void onChangeSliderValue(GuiSlider slider) {
        double value = round(slider.getValue());
        if (slider.id == LIGHT) {
            LimboFog.shaderLightTuning = value;
            slider.displayString = lightLabel(value);
            return;
        }
        if (shaders) LimboFog.shaderTuning = value;
        else LimboFog.tuning = value;
        slider.displayString = label(value);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == RESET) {
            slider.setValue(shaders ? LimboFog.SHADER_DEFAULT : DEFAULT);
            slider.updateSlider();
        } else if (button.id == LIGHT_RESET) {
            light.setValue(LimboFog.SHADER_LIGHT_DEFAULT);
            light.updateSlider();
        } else if (button.id == APPLY) {
            apply();
        } else if (button.id == DONE) {
            mc.displayGuiScreen(null);
        }
    }

    /**
     * Writes the values through the loaded config, which keeps the file's comments. A density is read back at the next
     * start and used until then; a pack's strength and light are used at once.
     */
    private void apply() {
        Configuration config = DimDoors.properties.Config;
        if (shaders) {
            save(config, DDProperties.LIMBO_SHADER_FOG, DimDoors.properties.LimboShaderFog, LimboFog.shaderStrength());
            save(config, DDProperties.LIMBO_SHADER_LIGHT, DimDoors.properties.LimboShaderLight, LimboFog.shaderLight());
            LimboFog.shaderTuning = -1;
            LimboFog.shaderLightTuning = -1;
        } else {
            double density = LimboFog.density();
            config.get(Configuration.CATEGORY_GENERAL, KEY, DEFAULT)
                .set(density);
            saved = density;
        }
        config.save();
        status = I18n.format(
            "dimdoors.limbofog.applied",
            config.getConfigFile()
                .getName());
    }

    /** Sets this pack's value in a pack name=value list, in memory and in the config. */
    private void save(Configuration config, String key, Map<String, Double> values, double value) {
        values.put(pack, value);
        List<String> entries = new ArrayList<String>();
        for (Map.Entry<String, Double> entry : values.entrySet()) entries.add(entry.getKey() + "=" + entry.getValue());
        config.get(Configuration.CATEGORY_GENERAL, key, new String[0])
            .set(entries.toArray(new String[0]));
    }

    @Override
    public void onGuiClosed() {
        // Unapplied tries don't outlive the screen
        LimboFog.shaderTuning = -1;
        LimboFog.shaderLightTuning = -1;
        LimboFog.tuning = saved == DimDoors.properties.LimboFogDensity ? -1 : saved;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // No darkening: Limbo stays in view behind a panel for the controls
        int bottom = (status.isEmpty() ? 76 : 88) + (shaders ? 24 : 0);
        drawRect(LEFT - 4, 4, LEFT + WIDTH + 4, bottom, 0x90000000);
        String title = shaders ? I18n.format("dimdoors.limbofog.title.pack", pack)
            : I18n.format("dimdoors.limbofog.title");
        drawString(fontRendererObj, fontRendererObj.trimStringToWidth(title, WIDTH), LEFT, 10, 0xFFFFFF);
        if (!status.isEmpty()) drawString(fontRendererObj, status, LEFT, bottom - 10, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static double round(double value) {
        return Math.round(value * 1000) / 1000.0;
    }

    private static String lightLabel(double value) {
        return I18n.format("dimdoors.limbofog.light", String.format("%.2f", value));
    }

    /** The density with how far one can see through it, or the pack's strength. */
    private String label(double value) {
        if (shaders) return value <= 0 ? I18n.format("dimdoors.limbofog.shaders.off")
            : I18n.format("dimdoors.limbofog.shaders", String.format("%.2f", value));
        if (value <= 0) return I18n.format("dimdoors.limbofog.off");
        return I18n
            .format("dimdoors.limbofog.density", String.format("%.3f", value), Math.round(LimboFog.sight(value)));
    }
}
