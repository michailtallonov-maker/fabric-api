package dev.customcrosshair.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import dev.customcrosshair.CooldownRenderer;
import dev.customcrosshair.CrosshairConfig;
import dev.customcrosshair.CrosshairRenderer;

/**
 * Settings for the attack cooldown indicator, with an animated preview.
 */
public class CooldownScreen extends BaseConfigScreen {
	private static final String[] COLOR_TARGETS = {"background", "fill", "ready"};
	/** Which color the sliders and palette edit; kept while the tab is rebuilt. */
	private static int colorTarget = 1;

	public CooldownScreen(Screen parent) {
		super(parent, 1);
	}

	@Override
	protected int getEditColor() {
		return switch (colorTarget) {
			case 0 -> config.cdBackgroundColor;
			case 2 -> config.cdReadyColor;
			default -> config.cdFillColor;
		};
	}

	@Override
	protected void setEditColor(int color) {
		switch (colorTarget) {
			case 0 -> config.cdBackgroundColor = color;
			case 2 -> config.cdReadyColor = color;
			default -> config.cdFillColor = color;
		}
	}

	@Override
	protected void initTab() {
		int h = this.rowHeight;

		// ---- Left column: look ----
		int lx = this.leftX;
		int y = MARGIN;

		this.addRenderableWidget(this.toggle(lx, y, COLUMN_WIDTH, "customcrosshair.option.attack_indicator",
				() -> config.attackIndicator, v -> config.attackIndicator = v));
		y += step;

		this.addRenderableWidget(this.choice(lx, y, COLUMN_WIDTH, "customcrosshair.option.cd_style", CooldownRenderer.Style.values().length,
				() -> config.cdStyle, v -> config.cdStyle = v,
				v -> Component.translatable(CooldownRenderer.Style.values()[v].translationKey())));
		y += step;

		// Changing the edited color needs fresh sliders.
		this.addRenderableWidget(Button.builder(
				Component.translatable("customcrosshair.option.cd_color",
						Component.translatable("customcrosshair.cd_color." + COLOR_TARGETS[colorTarget])),
				b -> {
					colorTarget = (colorTarget + 1) % COLOR_TARGETS.length;
					this.rebuild();
				}).bounds(lx, y, COLUMN_WIDTH, h).build());
		y += step;

		y = this.colorSliders(lx, y);

		this.addRenderableWidget(this.toggle(lx, y, COLUMN_WIDTH, "customcrosshair.option.cd_gradient",
				() -> config.cdGradient, v -> config.cdGradient = v));

		// ---- Right column: size and placement ----
		int rx = this.rightX;
		y = MARGIN;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.cd_length",
				2, 64, 1, () -> config.cdLength, v -> config.cdLength = (int) v, v -> String.valueOf((int) v)));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.cd_thickness",
				1, 16, 1, () -> config.cdThickness, v -> config.cdThickness = (int) v, v -> String.valueOf((int) v)));
		y += step;

		this.addRenderableWidget(this.choice(rx, y, COLUMN_WIDTH, "customcrosshair.option.cd_position", "customcrosshair.cd_position", 5,
				() -> config.cdPosition, v -> config.cdPosition = v));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.offset_x",
				-100, 100, 1, () -> config.cdOffsetX, v -> config.cdOffsetX = (int) v, v -> String.valueOf((int) v)));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.offset_y",
				-100, 100, 1, () -> config.cdOffsetY, v -> config.cdOffsetY = (int) v, v -> String.valueOf((int) v)));
		y += step;

		this.addRenderableWidget(this.toggle(rx, y, COLUMN_WIDTH, "customcrosshair.option.cd_reverse",
				() -> config.cdReverse, v -> config.cdReverse = v));
		y += step;

		this.addRenderableWidget(this.toggle(rx, y, COLUMN_WIDTH, "customcrosshair.option.cd_percent",
				() -> config.cdPercent, v -> config.cdPercent = v));
		y += step;

		this.addRenderableWidget(this.toggle(rx, y, COLUMN_WIDTH, "customcrosshair.option.cd_show_ready",
				() -> config.cdShowWhenReady, v -> config.cdShowWhenReady = v));
		y += step;

		this.addRenderableWidget(Button.builder(Component.translatable("customcrosshair.option.reset"), b -> {
			CrosshairConfig defaults = new CrosshairConfig();
			config.cdStyle = defaults.cdStyle;
			config.cdBackgroundColor = defaults.cdBackgroundColor;
			config.cdFillColor = defaults.cdFillColor;
			config.cdReadyColor = defaults.cdReadyColor;
			config.cdGradient = defaults.cdGradient;
			config.cdLength = defaults.cdLength;
			config.cdThickness = defaults.cdThickness;
			config.cdPosition = defaults.cdPosition;
			config.cdOffsetX = defaults.cdOffsetX;
			config.cdOffsetY = defaults.cdOffsetY;
			config.cdReverse = defaults.cdReverse;
			config.cdPercent = defaults.cdPercent;
			config.cdShowWhenReady = defaults.cdShowWhenReady;
			this.rebuild();
		}).bounds(rx, y, COLUMN_WIDTH, h).build());

		this.paletteY = this.height - MARGIN - 12 - PALETTE_SIZE - 2;
	}

	@Override
	protected void extractTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int left = this.centerLeft;
		int right = this.centerRight;
		int top = this.centerTop;
		int bottom = this.paletteY - 6;

		graphics.fill(left, top, right, bottom, 0xFF6A8CAF);
		frame(graphics, left, top, right - left, bottom - top, 0xFF000000);
		graphics.text(this.font, Component.translatable("customcrosshair.preview").getString(), left + 3, top + 3, 0xFFFFFFFF);

		// Charge for 1.6 s, then stay "ready" for 0.6 s.
		float progress = Math.min(1.0F, (System.currentTimeMillis() % 2200L) / 1600.0F);
		float zoom = 2.0F;
		float cx = (left + right) / 2.0F;
		float cy = (top + bottom) / 2.0F;
		float half = config.gridSize * config.scale / 2.0F;
		boolean fillStyle = config.attackIndicator && config.cdStyle == CooldownRenderer.Style.CROSSHAIR_FILL.ordinal()
				&& (progress < 1.0F || config.cdShowWhenReady);

		graphics.enableScissor(left + 1, top + 1, right - 1, bottom - 1);
		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		pose.scale(zoom, zoom);

		if (!fillStyle) {
			CrosshairRenderer.draw(graphics, config, 0, 0, config.scale);
		}

		if (config.attackIndicator) {
			CooldownRenderer.draw(graphics, config, 0, 0, half, progress, config.opacity);
		}

		pose.popMatrix();
		graphics.disableScissor();
	}

	@Override
	protected String hint() {
		return Component.translatable("customcrosshair.cd_hint").getString();
	}
}
