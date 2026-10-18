package dev.customcrosshair.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import dev.customcrosshair.CrosshairConfig;
import dev.customcrosshair.HitParticles;

/**
 * Settings for the particles shown when you hit an entity.
 */
public class ParticlesScreen extends BaseConfigScreen {
	public ParticlesScreen(Screen parent) {
		super(parent, 2);
	}

	@Override
	protected int getEditColor() {
		return config.particleColor;
	}

	@Override
	protected void setEditColor(int color) {
		config.particleColor = color;
	}

	@Override
	protected void initTab() {
		int h = this.rowHeight;

		// ---- Left column: what and when ----
		int lx = this.leftX;
		int y = MARGIN;

		this.addRenderableWidget(this.toggle(lx, y, COLUMN_WIDTH, "customcrosshair.option.particles",
				() -> config.particlesEnabled, v -> config.particlesEnabled = v));
		y += step;

		this.addRenderableWidget(this.choice(lx, y, COLUMN_WIDTH, "customcrosshair.option.particle_trigger", "customcrosshair.particle_trigger", 2,
				() -> config.particleTrigger, v -> config.particleTrigger = v));
		y += step;

		this.addRenderableWidget(this.toggle(lx, y, COLUMN_WIDTH, "customcrosshair.option.hide_vanilla_particles",
				() -> config.hideVanillaParticles, v -> config.hideVanillaParticles = v));
		y += step;

		this.addRenderableWidget(new ValueSlider(lx, y, COLUMN_WIDTH, h, "customcrosshair.option.particle_type",
				0, HitParticles.TYPES.size() - 1, 1, () -> config.particleType, v -> config.particleType = (int) v,
				v -> HitParticles.TYPES.get((int) v).name()));
		y += step;

		this.colorSliders(lx, y);

		// ---- Right column: look ----
		int rx = this.rightX;
		y = MARGIN;

		this.addRenderableWidget(this.toggle(rx, y, half, "customcrosshair.option.particle_tint",
				() -> config.particleTint, v -> config.particleTint = v));
		this.addRenderableWidget(this.toggle(rx + half + 2, y, half, "customcrosshair.option.particle_rainbow",
				() -> config.particleRainbow, v -> config.particleRainbow = v));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.scale",
				0.1, 5.0, 0.1, () -> config.particleSize, v -> config.particleSize = (float) v, v -> String.format("%.1fx", v)));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.particle_count",
				1, 100, 1, () -> config.particleCount, v -> config.particleCount = (int) v, v -> String.valueOf((int) v)));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.particle_speed",
				0, 3, 0.1, () -> config.particleSpeed, v -> config.particleSpeed = (float) v, v -> String.format("%.1fx", v)));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.particle_gravity",
				-1, 1, 0.05, () -> config.particleGravity, v -> config.particleGravity = (float) v, v -> String.format("%.2f", v)));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.particle_lifetime",
				0.2, 5, 0.1, () -> config.particleLifetime, v -> config.particleLifetime = (float) v, v -> String.format("%.1fx", v)));
		y += step;

		this.addRenderableWidget(Button.builder(Component.translatable("customcrosshair.option.particle_test"), b -> {
			this.onClose();
			HitParticles.spawnTest();
		}).bounds(rx, y, COLUMN_WIDTH, h).build());
		y += step;

		this.addRenderableWidget(Button.builder(Component.translatable("customcrosshair.option.reset"), b -> {
			CrosshairConfig defaults = new CrosshairConfig();
			config.particleTrigger = defaults.particleTrigger;
			config.hideVanillaParticles = defaults.hideVanillaParticles;
			config.particleType = defaults.particleType;
			config.particleColor = defaults.particleColor;
			config.particleTint = defaults.particleTint;
			config.particleRainbow = defaults.particleRainbow;
			config.particleSize = defaults.particleSize;
			config.particleCount = defaults.particleCount;
			config.particleSpeed = defaults.particleSpeed;
			config.particleGravity = defaults.particleGravity;
			config.particleLifetime = defaults.particleLifetime;
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

		graphics.fill(left, top, right, bottom, 0xFF1E1E28);
		frame(graphics, left, top, right - left, bottom - top, 0xFF000000);
		graphics.text(this.font, Component.translatable("customcrosshair.preview").getString(), left + 3, top + 3, 0xFFFFFFFF);
		graphics.text(this.font, HitParticles.TYPES.get(config.particleType).name(), left + 3, bottom - 12, 0xFFB0B0B0);

		// A rough sketch of the burst: color, size, count and speed.
		float cx = (left + right) / 2.0F;
		float cy = (top + bottom) / 2.0F;
		float maxRadius = Math.min(right - left, bottom - top) / 2.0F - 6;
		long period = Math.round(1000 * config.particleLifetime);
		float t = (System.currentTimeMillis() % period) / (float) period;
		float hueOffset = (System.currentTimeMillis() % 3000L) / 3000.0F;

		graphics.enableScissor(left + 1, top + 1, right - 1, bottom - 1);

		for (int i = 0; i < config.particleCount; i++) {
			// Deterministic pseudo-random direction per particle.
			double angle = i * 2.399963;
			float spread = 0.5F + ((i * 7919) % 100) / 200.0F;
			float distance = t * maxRadius * spread * Math.min(1.0F, 0.3F + config.particleSpeed * 0.7F);
			float px = cx + (float) Math.cos(angle) * distance;
			float py = cy + (float) Math.sin(angle) * distance + t * t * config.particleGravity * 40.0F;
			int size = Math.max(1, Math.round(3 * config.particleSize * (1.0F - t * 0.5F)));
			int color = config.particleRainbow
					? HitParticles.hsv(hueOffset + (float) i / config.particleCount, 0.8F, 1.0F)
					: (config.particleTint ? config.particleColor : 0xFFFFFFFF);
			int alpha = Math.round(255 * (1.0F - t));
			color = (alpha << 24) | (color & 0xFFFFFF);
			int x = Math.round(px - size / 2.0F);
			int y = Math.round(py - size / 2.0F);
			graphics.fill(x, y, x + size, y + size, color);
		}

		graphics.disableScissor();
	}

	@Override
	protected String hint() {
		return Component.translatable("customcrosshair.particle_hint").getString();
	}
}
