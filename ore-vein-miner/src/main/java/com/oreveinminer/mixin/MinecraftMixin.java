package com.oreveinminer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;

import com.oreveinminer.OreVeinMiner;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	/**
	 * Vanilla calls {@code stopDestroyBlock()} every tick the attack key is not held, which would reset the
	 * progress on the vein block being mined. Skip vanilla attack handling while a vein is being mined.
	 */
	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void oreveinminer$pauseVanillaAttack(boolean leftClick, CallbackInfo ci) {
		if (OreVeinMiner.isMining()) {
			ci.cancel();
		}
	}
}
