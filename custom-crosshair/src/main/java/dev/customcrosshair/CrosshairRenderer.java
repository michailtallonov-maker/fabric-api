package dev.customcrosshair;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

public final class CrosshairRenderer {
	private static final int[] OUTLINE_COLORS = {0, 0xFF000000, 0xFFFFFFFF};

	private CrosshairRenderer() {
	}

	/**
	 * Replacement for the vanilla crosshair HUD element.
	 */
	public static void extractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, HudElement vanilla) {
		CrosshairConfig config = CrosshairConfig.get();
		Minecraft minecraft = Minecraft.getInstance();

		if (!config.enabled || minecraft.player == null) {
			vanilla.extractRenderState(graphics, deltaTracker);
			return;
		}

		// F3: either keep our crosshair in place or let vanilla draw the debug axes.
		if (minecraft.debugEntries.isOverlayVisible() && !config.showInDebug) {
			vanilla.extractRenderState(graphics, deltaTracker);
			return;
		}

		// F5: vanilla hides the crosshair in third person, we keep it at the same spot unless disabled.
		if (!minecraft.options.getCameraType().isFirstPerson() && !config.showInThirdPerson) {
			return;
		}

		float size = config.gridSize * config.scale;
		float centerX = (float) Math.floor((graphics.guiWidth() - size) / 2.0F) + size / 2.0F + config.offsetX;
		float centerY = (float) Math.floor((graphics.guiHeight() - size) / 2.0F) + size / 2.0F + config.offsetY;

		draw(graphics, config, centerX, centerY, config.scale);

		if (config.attackIndicator) {
			float strength = minecraft.player.getAttackStrengthScale(0.0F);

			if (strength < 1.0F) {
				int width = 16;
				int x = Math.round(centerX) - width / 2;
				int y = Math.round(centerY + size / 2.0F) + 3;
				graphics.fill(x, y, x + width, y + 2, applyOpacity(0xFF404040, config.opacity));
				graphics.fill(x, y, x + Math.round(width * strength), y + 2, applyOpacity(0xFFFFFFFF, config.opacity));
			}
		}
	}

	/**
	 * Draws the crosshair with all transforms (scale, rotation, flips) around the given center point.
	 */
	public static void draw(GuiGraphicsExtractor graphics, CrosshairConfig config, float centerX, float centerY, float scale) {
		int size = config.gridSize;
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(centerX, centerY);
		pose.rotate((float) Math.toRadians(config.rotation));
		pose.scale(config.flipX ? -scale : scale, config.flipY ? -scale : scale);
		pose.translate(-size / 2.0F, -size / 2.0F);

		if (config.outline != 0) {
			int outlineColor = applyOpacity(OUTLINE_COLORS[config.outline], config.opacity);

			// Outline may stick out of the grid by one pixel on every side.
			for (int y = -1; y <= size; y++) {
				for (int x = -1; x <= size; x++) {
					if (config.getPixel(x, y) == 0 && hasNeighbour(config, x, y)) {
						graphics.fill(x, y, x + 1, y + 1, outlineColor);
					}
				}
			}
		}

		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int color = config.getPixel(x, y);

				if (color != 0) {
					graphics.fill(x, y, x + 1, y + 1, applyOpacity(color, config.opacity));
				}
			}
		}

		pose.popMatrix();
	}

	private static boolean hasNeighbour(CrosshairConfig config, int x, int y) {
		return config.getPixel(x - 1, y) != 0 || config.getPixel(x + 1, y) != 0
				|| config.getPixel(x, y - 1) != 0 || config.getPixel(x, y + 1) != 0;
	}

	public static int applyOpacity(int argb, float opacity) {
		int alpha = Math.round((argb >>> 24) * opacity);
		return (alpha << 24) | (argb & 0xFFFFFF);
	}
}
