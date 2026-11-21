package com.example.zoom;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.Locale;

public class ZoomSettingsScreen extends Screen {

    private final Screen parent;

    public ZoomSettingsScreen() {
        this(null);
    }

    public ZoomSettingsScreen(Screen parent) {
        super(new LiteralText("Zoom Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;

        this.addButton(new ConfigSlider(center - 155, 42, 150, 20,
                "Zoom", 0.05, 0.8,
                ZoomClient.getConfig().zoomFactor,
                value -> ZoomClient.getConfig().zoomFactor = (float) value,
                value -> String.format(Locale.US, "Zoom: %.0f%% FOV", value * 100.0)));

        this.addButton(new ConfigSlider(center + 5, 42, 150, 20,
                "Transition", 0.05, 2.0,
                ZoomClient.getConfig().transitionSeconds,
                value -> ZoomClient.getConfig().transitionSeconds = (float) value,
                value -> String.format(Locale.US, "Transition: %.2fs", value)));

        this.addButton(new ConfigSlider(center - 155, 68, 150, 20,
                "Horizontal sensitivity", 0.05, 1.0,
                ZoomClient.getConfig().horizontalSensitivityScale,
                value -> ZoomClient.getConfig().horizontalSensitivityScale = (float) value,
                value -> String.format(Locale.US, "Horizontal: %.0f%%", value * 100.0)));

        this.addButton(new ConfigSlider(center + 5, 68, 150, 20,
                "Vertical sensitivity", 0.05, 1.0,
                ZoomClient.getConfig().verticalSensitivityScale,
                value -> ZoomClient.getConfig().verticalSensitivityScale = (float) value,
                value -> String.format(Locale.US, "Vertical: %.0f%%", value * 100.0)));

        this.addButton(new ButtonWidget(center - 155, 94, 150, 20,
                new LiteralText(ZoomClient.getConfig().toggleMode ? "Key: Toggle" : "Key: Hold"),
                button -> {
                    ZoomClient.getConfig().toggleMode = !ZoomClient.getConfig().toggleMode;
                    button.setMessage(new LiteralText(
                            ZoomClient.getConfig().toggleMode ? "Key: Toggle" : "Key: Hold"));
                    ZoomClient.saveConfig();
                }));

        this.addButton(new ButtonWidget(center + 5, 94, 150, 20,
                new LiteralText(ZoomClient.getConfig().mouseWheelCycling
                        ? "Mouse Wheel: On" : "Mouse Wheel: Off"),
                button -> {
                    ZoomClient.getConfig().mouseWheelCycling =
                            !ZoomClient.getConfig().mouseWheelCycling;
                    button.setMessage(new LiteralText(
                            ZoomClient.getConfig().mouseWheelCycling
                                    ? "Mouse Wheel: On" : "Mouse Wheel: Off"));
                    ZoomClient.saveConfig();
                }));

        this.addButton(new ButtonWidget(center - 155, 120, 150, 20,
                new LiteralText("Reset Defaults"),
                button -> {
                    ZoomClient.Config defaults = ZoomClient.Config.defaults();
                    ZoomClient.getConfig().zoomFactor = defaults.zoomFactor;
                    ZoomClient.getConfig().transitionSeconds = defaults.transitionSeconds;
                    ZoomClient.getConfig().horizontalSensitivityScale =
                            defaults.horizontalSensitivityScale;
                    ZoomClient.getConfig().verticalSensitivityScale =
                            defaults.verticalSensitivityScale;
                    ZoomClient.getConfig().toggleMode = defaults.toggleMode;
                    ZoomClient.getConfig().mouseWheelCycling = defaults.mouseWheelCycling;
                    ZoomClient.saveConfig();
                    this.client.openScreen(new ZoomSettingsScreen(this.parent));
                }));

        this.addButton(new ButtonWidget(center + 5, 120, 150, 20,
                new LiteralText("Done"),
                button -> this.onClose()));
    }

    @Override
    public void onClose() {
        ZoomClient.saveConfig();
        if (this.parent != null) {
            this.client.openScreen(this.parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 16, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    private interface ConfigValueSetter {
        void set(double value);
    }

    private interface ConfigMessageBuilder {
        String build(double value);
    }

    private class ConfigSlider extends SliderWidget {
        private final double minimum;
        private final double maximum;
        private final ConfigValueSetter setter;
        private final ConfigMessageBuilder messageBuilder;

        ConfigSlider(int x, int y, int width, int height, String ignoredName,
                     double minimum, double maximum, double initialValue,
                     ConfigValueSetter setter, ConfigMessageBuilder messageBuilder) {
            super(x, y, width, height, LiteralText.EMPTY, 0.0);
            this.minimum = minimum;
            this.maximum = maximum;
            this.setter = setter;
            this.messageBuilder = messageBuilder;
            this.value = (initialValue - minimum) / (maximum - minimum);
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            double value = minimum + this.value * (maximum - minimum);
            this.setMessage(new LiteralText(messageBuilder.build(value)));
        }

        @Override
        protected void applyValue() {
            setter.set(minimum + this.value * (maximum - minimum));
            ZoomClient.saveConfig();
        }
    }
}
