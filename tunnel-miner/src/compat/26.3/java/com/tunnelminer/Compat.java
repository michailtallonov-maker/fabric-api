package com.tunnelminer;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLScancode;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/** Code that differs between Minecraft versions (this copy: 26.3). */
final class Compat {
	private Compat() {
	}

	static KeyMapping toggleKey(String name, KeyMapping.Category category) {
		return new KeyMapping(name, InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_B, category);
	}

	static void swingMainHand(LocalPlayer player) {
		player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getSwingAnimation(), false);
	}
}
