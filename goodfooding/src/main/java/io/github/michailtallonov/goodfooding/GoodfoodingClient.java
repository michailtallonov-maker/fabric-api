package io.github.michailtallonov.goodfooding;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLScancode;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class GoodfoodingClient implements ClientModInitializer {
	public static final String MOD_ID = "goodfooding";

	private static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "goodfooding"));
		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.goodfooding.toggle", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_F4, category));

		// Runs before vanilla processes key bindings and ticks the player, so blocked inputs never reach the game.
		ClientTickEvents.START_CLIENT_TICK.register(Goodfooding::tick);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				Goodfooding.toggle(client);
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> Goodfooding.disable(client, false)));
	}
}
