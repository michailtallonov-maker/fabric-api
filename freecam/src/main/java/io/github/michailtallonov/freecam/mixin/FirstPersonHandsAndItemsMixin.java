package io.github.michailtallonov.freecam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;

import io.github.michailtallonov.freecam.Freecam;
import io.github.michailtallonov.freecam.HandHider;

/**
 * Lower the first-person hands out of view while flying around, they don't belong to the free camera.
 */
@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin {
	@Inject(method = "tick", at = @At("TAIL"), require = 0)
	private void freecam$hideHands(LocalPlayer player, CallbackInfo ci) {
		if (Freecam.isEnabled()) {
			HandHider.lower(this);
		}
	}
}
