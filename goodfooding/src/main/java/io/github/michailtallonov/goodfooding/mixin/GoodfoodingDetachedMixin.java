package io.github.michailtallonov.goodfooding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;

import io.github.michailtallonov.goodfooding.Goodfooding;

/**
 * Treat the goodfooding as detached so the player's own body is rendered.
 */
@Mixin(Camera.class)
public abstract class GoodfoodingDetachedMixin {
	@Inject(method = "isDetached", at = @At("HEAD"), cancellable = true, require = 0)
	private void goodfooding$detached(CallbackInfoReturnable<Boolean> cir) {
		if (Goodfooding.isEnabled()) {
			cir.setReturnValue(true);
		}
	}
}
