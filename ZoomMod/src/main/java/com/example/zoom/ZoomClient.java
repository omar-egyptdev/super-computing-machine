package com.example.zoom;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.Color;

/**
 * Client-side entrypoint for the Zoom mod.
 *
 * Holds the keybinding state and computes the smoothed FOV multiplier that the
 * GameRenderer mixin applies. The transition is time-based (not tick-based) so
 * it stays smooth regardless of framerate.
 */
public class ZoomClient implements ClientModInitializer {

    public static final String MOD_ID = "zoom";

    /** Registered keybinding (default: C). */
    public static KeyBinding zoomKey;
    public static KeyBinding settingsKey;

    /** 0.0 = not zoomed, 1.0 = fully zoomed. */
    private static float progress = 0.0f;
    private static boolean toggleActive = false;
    private static boolean zoomKeyDown = false;

    /** Timestamp of the previous frame (nanoTime), used for frame-rate independent easing. */
    private static long lastFrameNanos = 0L;
    private static long lastAnimationUpdateNanos = 0L;
    private static boolean welcomedForWorld = false;
    private static int welcomeAnimationTicks = 0;
    private static int welcomeAnimationFrame = 0;

    public static final float[] ZOOM_PRESETS = { 0.5f, 0.35f, 0.2f, 0.1f };
    private static int presetIndex = 2;

    private static Config config = Config.defaults();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;

    public static Config getConfig() {
        return config;
    }

    public static void saveConfig() {
        if (configPath == null) {
            return;
        }
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void cyclePreset(double direction) {
        presetIndex += direction > 0.0D ? -1 : 1;
        if (presetIndex < 0) presetIndex = ZOOM_PRESETS.length - 1;
        if (presetIndex >= ZOOM_PRESETS.length) presetIndex = 0;
        config.zoomFactor = ZOOM_PRESETS[presetIndex];
        saveConfig();
    }

    public static float getProgress() {
        return progress;
    }

    public static float getEasedProgress() {
        return progress * progress * (3.0f - 2.0f * progress);
    }

    @Override
    public void onInitializeClient() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("zoom.json");
        loadConfig();
        presetIndex = nearestPresetIndex(config.zoomFactor);

        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zoom.zoom",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category.zoom"
        ));
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zoom.settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "category.zoom"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (settingsKey.isPressed() && client.currentScreen == null) {
                client.openScreen(new ZoomSettingsScreen());
            }

            if (client.player == null || client.world == null) {
                welcomedForWorld = false;
                welcomeAnimationTicks = 0;
                return;
            }

            if (!welcomedForWorld) {
                welcomedForWorld = true;
                welcomeAnimationTicks = 100;
                welcomeAnimationFrame = 0;
                sendWelcomeMessage(client);
            }

            if (welcomeAnimationTicks > 0) {
                client.player.sendMessage(
                        buildAnimatedWelcome("  ZOOM  |  Hold C to zoom  |  Scroll to change level  ",
                                welcomeAnimationFrame),
                        true
                );
                welcomeAnimationTicks--;
                welcomeAnimationFrame++;
            }
        });
    }

    private static void sendWelcomeMessage(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(buildRainbowText("  ~ Zoom Mod 1.1.0 ~  ", 0), false);
        client.player.sendMessage(
                coloredText("Hold C", 0x55FF55)
                        .append(coloredText(" to zoom your view.", 0xFFFFFF)),
                false
        );
        client.player.sendMessage(
                coloredText("Mouse Wheel", 0x55FFFF)
                        .append(coloredText(" while zoomed changes zoom levels.", 0xFFFFFF)),
                false
        );
        client.player.sendMessage(
                coloredText("Press Z", 0xFFAA00)
                        .append(coloredText(" to open settings.", 0xFFFFFF)),
                false
        );
        client.player.sendMessage(
                coloredText("Configure", 0xFF55FF)
                        .append(coloredText(" zoom strength, transition, sensitivity and hold/toggle mode.", 0xAAAAAA)),
                false
        );
    }

    private static MutableText coloredText(String text, int rgb) {
        return new LiteralText(text).setStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(rgb))
                        .withBold(rgb == 0x55FF55 || rgb == 0xFFAA00)
        );
    }

    private static Text buildRainbowText(String text, int phase) {
        MutableText result = new LiteralText("");
        for (int i = 0; i < text.length(); i++) {
            float hue = ((i * 0.045f) + phase * 0.025f) % 1.0f;
            int rgb = Color.HSBtoRGB(hue, 0.78f, 1.0f) & 0xFFFFFF;
            result.append(new LiteralText(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }
        return result;
    }

    private static Text buildAnimatedWelcome(String text, int frame) {
        MutableText result = new LiteralText("");
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            float hue = ((i * 0.035f) + frame * 0.055f) % 1.0f;
            int rgb = Color.HSBtoRGB(hue, 0.72f, 1.0f) & 0xFFFFFF;
            result.append(new LiteralText(String.valueOf(character))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))
                            .withBold(character != ' ')));
        }
        return result;
    }

    private void loadConfig() {
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                Config loaded = GSON.fromJson(reader, Config.class);
                if (loaded != null) {
                    config = loaded.sanitized();
                }
            } catch (Exception ignored) {
            }
        }
        saveConfig();
    }

    private static int nearestPresetIndex(float factor) {
        int nearest = 0;
        float nearestDistance = Float.MAX_VALUE;
        for (int i = 0; i < ZOOM_PRESETS.length; i++) {
            float distance = Math.abs(ZOOM_PRESETS[i] - factor);
            if (distance < nearestDistance) {
                nearest = i;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    /**
     * Advances the zoom animation by one frame and returns the FOV multiplier.
     *
     * Called from GameRendererMixin every time the vanilla FOV is computed.
     * Because the multiplier is derived from elapsed wall-clock time, calling
     * this more than once per frame (some vanilla paths call getFov several
     * times) still integrates to the correct total transition.
     */
    public static double getFovMultiplier() {
        final long now = System.nanoTime();
        if (lastFrameNanos != 0L
                && now - lastFrameNanos < 1_000_000L) {
            return 1.0 - (1.0 - config.zoomFactor) * getEasedProgress();
        }
        lastFrameNanos = now;

        final MinecraftClient client = MinecraftClient.getInstance();
        boolean keyPressed = zoomKey != null && zoomKey.isPressed();
        if (config.toggleMode) {
            if (keyPressed && !zoomKeyDown) {
                toggleActive = !toggleActive;
            }
            zoomKeyDown = keyPressed;
        } else {
            zoomKeyDown = keyPressed;
        }

        final boolean wantsZoom = client.currentScreen == null
                && (config.toggleMode ? toggleActive : keyPressed);

        final float delta = lastAnimationUpdateNanos == 0L
                ? 0.0f
                : (now - lastAnimationUpdateNanos) / 1_000_000_000.0f;
        lastAnimationUpdateNanos = now;
        final float clampedDelta = Math.max(0.0f, Math.min(0.1f, delta));

        final float speed = 1.0f / config.transitionSeconds;
        if (wantsZoom) {
            progress = Math.min(1.0f, progress + speed * clampedDelta);
        } else {
            progress = Math.max(0.0f, progress - speed * clampedDelta);
        }
        return 1.0 - (1.0 - config.zoomFactor) * getEasedProgress();
    }

    public static double getSensitivityScale(boolean horizontal) {
        float amount = horizontal
                ? config.horizontalSensitivityScale
                : config.verticalSensitivityScale;
        return 1.0D - (1.0D - amount) * getEasedProgress();
    }

    public static class Config {
        public float zoomFactor = 0.20f;
        public float transitionSeconds = 0.15f;
        public float horizontalSensitivityScale = 0.35f;
        public float verticalSensitivityScale = 0.35f;
        public boolean toggleMode = false;
        public boolean mouseWheelCycling = true;

        public static Config defaults() {
            return new Config();
        }

        public Config sanitized() {
            zoomFactor = Math.max(0.05f, Math.min(0.8f, zoomFactor));
            transitionSeconds = Math.max(0.05f, Math.min(2.0f, transitionSeconds));
            horizontalSensitivityScale = Math.max(0.05f, Math.min(1.0f, horizontalSensitivityScale));
            verticalSensitivityScale = Math.max(0.05f, Math.min(1.0f, verticalSensitivityScale));
            return this;
        }
    }
}
