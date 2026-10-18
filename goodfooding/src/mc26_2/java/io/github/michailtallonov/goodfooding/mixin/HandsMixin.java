package io.github.michailtallonov.goodfooding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.ItemInHandRenderer;

import io.github.michailtallonov.goodfooding.Goodfooding;
import io.github.michailtallonov.goodfooding.HandHider;

/**
 * Lower the first-person hands out of view while flying around, they don't belong to the goodfooding.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class HandsMixin {
	@Inject(method = "tick", at = @At("TAIL"), require = 0)
	private void goodfooding$hideHands(CallbackInfo ci) {
		if (Goodfooding.isEnabled()) {
			HandHider.lower(this);
		}
	}
}
