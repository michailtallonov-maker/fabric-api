package com.tunnelminer;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/** Code that differs between Minecraft versions (this copy: 26.2). */
final class Compat {
	private Compat() {
	}

	static KeyMapping toggleKey(String name, KeyMapping.Category category) {
		return new KeyMapping(name, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, category);
	}

	static void swingMainHand(LocalPlayer player) {
		player.swing(InteractionHand.MAIN_HAND);
	}
}
