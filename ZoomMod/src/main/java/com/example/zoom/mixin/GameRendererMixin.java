package com.example.zoom.mixin;

import com.example.zoom.ZoomClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts vanilla FOV calculation and applies the zoom multiplier.
 *
 * Target (Yarn 1.16.5):
 *   private double getFov(Camera camera, float tickDelta, boolean changingFov)
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(
            method = "getFov(Lnet/minecraft/client/render/Camera;FZ)D",
            at = @At("RETURN"),
            cancellable = true
    )
    private void zoom$modifyFov(Camera camera, float tickDelta, boolean changingFov,
                                CallbackInfoReturnable<Double> cir) {
        final double multiplier = ZoomClient.getFovMultiplier();
        if (multiplier != 1.0) {
            cir.setReturnValue(cir.getReturnValue() * multiplier);
        }
    }
}
