package dev.customcrosshair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Draws the attack cooldown ("recharge") indicator in one of several styles.
 */
public final class CooldownRenderer {
	private static final Identifier VANILLA_BACKGROUND = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_background");
	private static final Identifier VANILLA_PROGRESS = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_progress");
	private static final Identifier VANILLA_FULL = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_full");
	private static final int GAP = 2;

	public enum Style {
		/** Horizontal bar. */
		BAR,
		/** Vertical bar, fills from bottom to top. */
		VERTICAL_BAR,
		/** Ring that fills clockwise starting at the top. */
		RING,
		/** Your own crosshair fills up from the bottom. */
		CROSSHAIR_FILL,
		/** Vanilla sprites. */
		VANILLA;

		public String translationKey() {
			return "customcrosshair.cd_style." + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	private CooldownRenderer() {
	}

	/**
	 * Draws the indicator around a crosshair centered at {@code centerX/centerY} with the given half size.
	 *
	 * @param progress attack strength, 0..1
	 */
	public static void draw(GuiGraphicsExtractor graphics, CrosshairConfig config, float centerX, float centerY,
			float crosshairHalf, float progress, float opacity) {
		boolean ready = progress >= 1.0F;

		if (ready && !config.cdShowWhenReady) return;

		Style style = Style.values()[config.cdStyle];
		float shown = ready ? 1.0F : (config.cdReverse ? 1.0F - progress : progress);
		int background = CrosshairRenderer.applyOpacity(config.cdBackgroundColor, opacity);
		int fill = CrosshairRenderer.applyOpacity(fillColor(config, progress), opacity);

		if (style == Style.CROSSHAIR_FILL) {
			// The crosshair itself was skipped by the caller; draw it filled to the current progress.
			CrosshairRenderer.drawFilled(graphics, config, centerX, centerY, config.scale, shown, background, fill, opacity);
			drawPercent(graphics, config, centerX, centerY + crosshairHalf + GAP, progress, ready, opacity);
			return;
		}

		int w;
		int h;

		switch (style) {
			case VERTICAL_BAR -> {
				w = config.cdThickness;
				h = config.cdLength;
			}
			case RING -> {
				w = config.cdLength;
				h = config.cdLength;
			}
			case VANILLA -> {
				w = 16;
				h = 8;
			}
			default -> {
				w = config.cdLength;
				h = config.cdThickness;
			}
		}

		// Top-left corner of the indicator box, relative to the crosshair.
		float x;
		float y;

		switch (config.cdPosition) {
			case 1 -> { // above
				x = centerX - w / 2.0F;
				y = centerY - crosshairHalf - GAP - h;
			}
			case 2 -> { // left
				x = centerX - crosshairHalf - GAP - w;
				y = centerY - h / 2.0F;
			}
			case 3 -> { // right
				x = centerX + crosshairHalf + GAP;
				y = centerY - h / 2.0F;
			}
			case 4 -> { // center
				x = centerX - w / 2.0F;
				y = centerY - h / 2.0F;
			}
			default -> { // below
				x = centerX - w / 2.0F;
				y = centerY + crosshairHalf + GAP;
			}
		}

		int ix = Math.round(x) + config.cdOffsetX;
		int iy = Math.round(y) + config.cdOffsetY;

		switch (style) {
			case BAR -> {
				graphics.fill(ix, iy, ix + w, iy + h, background);
				graphics.fill(ix, iy, ix + Math.round(w * shown), iy + h, fill);
			}
			case VERTICAL_BAR -> {
				graphics.fill(ix, iy, ix + w, iy + h, background);
				int filled = Math.round(h * shown);
				graphics.fill(ix, iy + h - filled, ix + w, iy + h, fill);
			}
			case RING -> drawRing(graphics, ix + w / 2.0F, iy + h / 2.0F, w / 2.0F, config.cdThickness, shown, background, fill);
			case VANILLA -> {
				if (ready) {
					graphics.blitSprite(RenderPipelines.GUI_TEXTURED, VANILLA_FULL, ix, iy, 16, 16);
				} else {
					graphics.blitSprite(RenderPipelines.GUI_TEXTURED, VANILLA_BACKGROUND, ix, iy, 16, 4);
					graphics.blitSprite(RenderPipelines.GUI_TEXTURED, VANILLA_PROGRESS, 16, 4, 0, 0, ix, iy, (int) (shown * 17.0F), 4);
				}
			}
			default -> {
			}
		}

		drawPercent(graphics, config, ix + w / 2.0F, iy + h + 1, progress, ready, opacity);
	}

	private static int fillColor(CrosshairConfig config, float progress) {
		if (progress >= 1.0F) return config.cdReadyColor;
		if (!config.cdGradient) return config.cdFillColor;
		return lerpColor(config.cdFillColor, config.cdReadyColor, progress);
	}

	private static void drawPercent(GuiGraphicsExtractor graphics, CrosshairConfig config, float x, float y, float progress, boolean ready, float opacity) {
		if (!config.cdPercent || ready) return;

		String text = Math.round(progress * 100) + "%";
		int color = CrosshairRenderer.applyOpacity(0xFFFFFFFF, opacity);
		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(0.5F, 0.5F);
		graphics.centeredText(Minecraft.getInstance().font, text, 0, 0, color);
		pose.popMatrix();
	}

	/**
	 * Ring drawn pixel by pixel; the arc from 12 o'clock clockwise up to {@code shown} uses the fill color.
	 */
	private static void drawRing(GuiGraphicsExtractor graphics, float cx, float cy, float radius, int thickness,
			float shown, int background, int fill) {
		float inner = Math.max(0.0F, radius - thickness);
		float limit = (float) (Math.PI * 2.0 * shown);
		int x0 = (int) Math.floor(cx - radius);
		int y0 = (int) Math.floor(cy - radius);
		int x1 = (int) Math.ceil(cx + radius);
		int y1 = (int) Math.ceil(cy + radius);

		for (int py = y0; py < y1; py++) {
			for (int px = x0; px < x1; px++) {
				float dx = px + 0.5F - cx;
				float dy = py + 0.5F - cy;
				float distance = (float) Math.sqrt(dx * dx + dy * dy);

				if (distance > radius || distance < inner) continue;

				float angle = (float) Math.atan2(dx, -dy);
				if (angle < 0) angle += (float) (Math.PI * 2.0);

				graphics.fill(px, py, px + 1, py + 1, angle <= limit ? fill : background);
			}
		}
	}

	public static int lerpColor(int from, int to, float t) {
		t = Math.clamp(t, 0.0F, 1.0F);
		int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
		int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
