package io.github.michailtallonov.freecam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;

import io.github.michailtallonov.freecam.Freecam;

/**
 * Treat the free camera as detached so the player's own body is rendered.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Inject(method = "isDetached", at = @At("HEAD"), cancellable = true, require = 0)
	private void freecam$detached(CallbackInfoReturnable<Boolean> cir) {
		if (Freecam.isEnabled()) {
			cir.setReturnValue(true);
		}
	}
}
