package io.github.michailtallonov.goodfooding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import io.github.michailtallonov.goodfooding.Goodfooding;

/**
 * Mouse look calls {@code turn} on the local player; while goodfooding is on, rotate the goodfooding view instead.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void goodfooding$redirectTurn(double yRot, double xRot, CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer && Goodfooding.isEnabled()) {
			Goodfooding.turn(yRot, xRot);
			ci.cancel();
		}
	}
}
