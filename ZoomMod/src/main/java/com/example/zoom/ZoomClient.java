package com.example.zoom;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side entrypoint for the Zoom mod.
 *
 * Holds the keybinding state and computes the smoothed FOV multiplier that the
 * GameRenderer mixin applies. The transition is time-based (not tick-based) so
 * it stays smooth regardless of framerate.
 */
public class ZoomClient implements ClientModInitializer {

    public static final String MOD_ID = "zoom";

    /** FOV is multiplied by this when fully zoomed (0.2 => 5x zoom). */
    public static final float ZOOM_FACTOR = 0.2f;

    /** Seconds for the full zoom-in / zoom-out transition. */
    public static final float TRANSITION_SECONDS = 0.15f;

    /** Registered keybinding (default: C). */
    public static KeyBinding zoomKey;

    /** 0.0 = not zoomed, 1.0 = fully zoomed. */
    private static float progress = 0.0f;

    /** Timestamp of the previous frame (nanoTime), used for frame-rate independent easing. */
    private static long lastFrameNanos = 0L;

    @Override
    public void onInitializeClient() {
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zoom.zoom",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category.zoom"
        ));
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
        final float delta;

        if (lastFrameNanos == 0L) {
            delta = 0.0f;
        } else {
            delta = (now - lastFrameNanos) / 1_000_000_000.0f;
        }
        lastFrameNanos = now;

        // Clamp to avoid a giant jump after the window was frozen / alt-tabbed.
        final float clampedDelta = Math.max(0.0f, Math.min(0.1f, delta));

        final MinecraftClient client = MinecraftClient.getInstance();
        final boolean wantsZoom = client.currentScreen == null
                && zoomKey != null
                && zoomKey.isPressed();

        final float speed = 1.0f / TRANSITION_SECONDS;
        if (wantsZoom) {
            progress = Math.min(1.0f, progress + speed * clampedDelta);
        } else {
            progress = Math.max(0.0f, progress - speed * clampedDelta);
        }

        // Smoothstep easing (3p^2 - 2p^3) for a non-linear, "cool" feel.
        final float eased = progress * progress * (3.0f - 2.0f * progress);

        // Interpolate between 1.0 (no zoom) and ZOOM_FACTOR.
        return 1.0 - (1.0 - ZOOM_FACTOR) * eased;
    }
}
