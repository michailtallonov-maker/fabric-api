package io.github.michailtallonov.freecam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import io.github.michailtallonov.freecam.Freecam;

/**
 * Mouse look calls {@code turn} on the local player; while freecam is on, rotate the camera instead.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void freecam$redirectTurn(double yRot, double xRot, CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer && Freecam.isEnabled()) {
			Freecam.turn(yRot, xRot);
			ci.cancel();
		}
	}
}
