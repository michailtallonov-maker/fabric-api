package io.github.michailtallonov.goodfooding;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLScancode;

import net.minecraft.client.KeyMapping;

/**
 * Bits that differ between Minecraft versions. Each supported version has its own copy of this class.
 */
public final class VersionCompat {
	private VersionCompat() {
	}

	/** Minecraft 26.3 uses SDL scancodes for keyboard keys. */
	public static KeyMapping createToggleKey(String name, KeyMapping.Category category) {
		return new KeyMapping(name, InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_F4, category);
	}
}
