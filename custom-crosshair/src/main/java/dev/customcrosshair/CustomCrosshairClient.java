package dev.customcrosshair;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

import dev.customcrosshair.screen.BaseConfigScreen;

public class CustomCrosshairClient implements ClientModInitializer {
	public static final String MOD_ID = "customcrosshair";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		CrosshairConfig.get();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		KeyMapping openEditor = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.customcrosshair.open_editor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
		KeyMapping toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.customcrosshair.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openEditor.consumeClick()) {
				client.gui.setScreen(BaseConfigScreen.openLast(client.gui.screen()));
			}

			while (toggle.consumeClick()) {
				CrosshairConfig config = CrosshairConfig.get();
				config.enabled = !config.enabled;
				config.save();
			}
		});

		// Fires on the client (and on the integrated server in singleplayer, which we ignore).
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (player instanceof LocalPlayer) {
				HitParticles.onAttack(entity);
			}

			return InteractionResult.PASS;
		});

		// Replace the vanilla crosshair. The vanilla element hides itself in F5 and swaps to the
		// debug axes in F3; ours is drawn at the same place regardless (configurable).
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR,
				vanilla -> (graphics, deltaTracker) -> CrosshairRenderer.extractHud(graphics, deltaTracker, vanilla));
	}
}
