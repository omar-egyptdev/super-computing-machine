package com.example.zoom.mixin;

import com.example.zoom.ZoomClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Inject(
            method = "onMouseScroll",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zoom$cyclePreset(long window, double horizontal, double vertical,
                                  CallbackInfo ci) {
        if (!ZoomClient.getConfig().mouseWheelCycling
                || vertical == 0.0D
                || ZoomClient.getProgress() <= 0.01f
                || MinecraftClient.getInstance().currentScreen != null) {
            return;
        }

        ZoomClient.cyclePreset(vertical);
        ci.cancel();
    }

    @ModifyArgs(
            method = "updateMouse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
            )
    )
    private void zoom$scaleLookSensitivity(Args args) {
        args.set(0, (Double) args.get(0) * ZoomClient.getSensitivityScale(true));
        args.set(1, (Double) args.get(1) * ZoomClient.getSensitivityScale(false));
    }
}
