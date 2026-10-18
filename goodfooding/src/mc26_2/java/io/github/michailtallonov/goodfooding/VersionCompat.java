package io.github.michailtallonov.goodfooding;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;

/**
 * Bits that differ between Minecraft versions. Each supported version has its own copy of this class.
 */
public final class VersionCompat {
	private VersionCompat() {
	}

	/** Minecraft 26.2 still uses GLFW key codes. */
	public static KeyMapping createToggleKey(String name, KeyMapping.Category category) {
		return new KeyMapping(name, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F4, category);
	}
}
