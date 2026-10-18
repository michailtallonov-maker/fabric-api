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
		float progress = minecraft.player.getAttackStrengthScale(0.0F);
		boolean fillStyle = config.attackIndicator && config.cdStyle == CooldownRenderer.Style.CROSSHAIR_FILL.ordinal()
				&& (progress < 1.0F || config.cdShowWhenReady);

		// The "crosshair fill" indicator draws the crosshair itself.
		if (!fillStyle) {
			draw(graphics, config, centerX, centerY, config.scale);
		}

		if (config.attackIndicator) {
			CooldownRenderer.draw(graphics, config, centerX, centerY, size / 2.0F, progress, config.opacity);
		}
	}

	/**
	 * Draws the crosshair with all transforms (scale, rotation, flips) around the given center point.
	 */
	public static void draw(GuiGraphicsExtractor graphics, CrosshairConfig config, float centerX, float centerY, float scale) {
		drawFilled(graphics, config, centerX, centerY, scale, -1.0F, 0, 0, config.opacity);
	}

	/**
	 * Same as {@link #draw}, but when {@code fill >= 0} the drawn pixels use {@code emptyColor} and the bottom
	 * {@code fill} part of the grid uses {@code fillColor} (used by the cooldown indicator).
	 */
	public static void drawFilled(GuiGraphicsExtractor graphics, CrosshairConfig config, float centerX, float centerY, float scale,
			float fill, int emptyColor, int fillColor, float opacity) {
		int size = config.gridSize;
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(centerX, centerY);
		pose.rotate((float) Math.toRadians(config.rotation));
		pose.scale(config.flipX ? -scale : scale, config.flipY ? -scale : scale);
		pose.translate(-size / 2.0F, -size / 2.0F);

		if (config.outline != 0) {
			int outlineColor = applyOpacity(OUTLINE_COLORS[config.outline], opacity);

			// Outline may stick out of the grid by one pixel on every side.
			for (int y = -1; y <= size; y++) {
				for (int x = -1; x <= size; x++) {
					if (config.getPixel(x, y) == 0 && hasNeighbour(config, x, y)) {
						graphics.fill(x, y, x + 1, y + 1, outlineColor);
					}
				}
			}
		}

		// Rows at or below this one count as "filled".
		float fillFrom = size - fill * size;

		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int color = config.getPixel(x, y);
				if (color == 0) continue;

				if (fill >= 0.0F) {
					color = y + 1 > fillFrom ? fillColor : emptyColor;
					graphics.fill(x, y, x + 1, y + 1, color);
				} else {
					graphics.fill(x, y, x + 1, y + 1, applyOpacity(color, opacity));
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
