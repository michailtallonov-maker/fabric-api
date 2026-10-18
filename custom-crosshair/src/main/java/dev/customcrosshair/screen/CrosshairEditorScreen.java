package dev.customcrosshair.screen;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import dev.customcrosshair.CrosshairConfig;
import dev.customcrosshair.CrosshairPresets;
import dev.customcrosshair.CrosshairRenderer;

/**
 * Pixel editor for the crosshair plus all display settings.
 *
 * <p>Left mouse button paints, right mouse button erases, middle mouse button picks a color.
 */
public class CrosshairEditorScreen extends Screen {
	private static final int MARGIN = 6;
	private static final int COLUMN_WIDTH = 124;
	private static final int PALETTE_SIZE = 10;
	private static final int[] PALETTE = {
			0xFFFFFFFF, 0xFFA0A0A0, 0xFF000000, 0xFFFF3030, 0xFFFF9020, 0xFFFFE030,
			0xFF60FF40, 0xFF20A040, 0xFF30E0FF, 0xFF3060FF, 0xFFA040FF, 0xFFFF40C0,
	};

	private final Screen parent;
	private final CrosshairConfig config = CrosshairConfig.get();
	private CrosshairPresets preset = CrosshairPresets.CROSS;

	// Layout, computed in init().
	private int gridX;
	private int gridY;
	private int cell;
	private int paletteY;
	private int previewX;
	private int previewY;
	private int previewW;
	private int previewH;

	/** -1 when not painting, otherwise the mouse button that started the stroke. */
	private int paintButton = -1;

	public CrosshairEditorScreen(Screen parent) {
		super(Component.translatable("customcrosshair.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int rows = 11;
		int step = Math.clamp((this.height - MARGIN * 2) / rows, 14, 22);
		int h = step - 2;
		int half = (COLUMN_WIDTH - 2) / 2;

		// ---- Left column: drawing ----
		int lx = MARGIN;
		int y = MARGIN;

		this.addRenderableWidget(new ValueSlider(lx, y, COLUMN_WIDTH, h, "customcrosshair.option.grid_size",
				CrosshairConfig.MIN_GRID, CrosshairConfig.MAX_GRID, 2, () -> config.gridSize,
				v -> {
					config.resize((int) v);
					this.layoutGrid();
				}, v -> (int) v + "x" + (int) v));
		y += step;

		y = this.colorSlider(lx, y, step, h, "customcrosshair.option.red", 16);
		y = this.colorSlider(lx, y, step, h, "customcrosshair.option.green", 8);
		y = this.colorSlider(lx, y, step, h, "customcrosshair.option.blue", 0);
		y = this.colorSlider(lx, y, step, h, "customcrosshair.option.alpha", 24);

		this.addRenderableWidget(this.cycleButton(lx, y, COLUMN_WIDTH, h,
				() -> Component.translatable("customcrosshair.option.mirror",
						Component.translatable("customcrosshair.mirror." + config.mirrorMode)),
				() -> config.mirrorMode = (config.mirrorMode + 1) % 4));
		y += step;

		this.addRenderableWidget(this.cycleButton(lx, y, COLUMN_WIDTH, h,
				() -> Component.translatable("customcrosshair.option.preset", Component.translatable(preset.translationKey())),
				() -> {
					preset.apply(config);
					preset = preset.next();
				}));
		y += step;

		this.addRenderableWidget(Button.builder(Component.translatable("customcrosshair.option.clear"), b -> config.clear())
				.bounds(lx, y, half, h).build());
		this.addRenderableWidget(Button.builder(Component.translatable("customcrosshair.option.recolor"), b -> config.recolor())
				.bounds(lx + half + 2, y, half, h).build());
		y += step;

		int doneY = this.height - MARGIN - h;
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
				.bounds(lx, doneY, COLUMN_WIDTH, h).build());

		this.previewX = lx;
		this.previewY = y + 2;
		this.previewW = COLUMN_WIDTH;
		this.previewH = Math.max(0, doneY - 4 - this.previewY);

		// ---- Right column: display ----
		int rx = this.width - MARGIN - COLUMN_WIDTH;
		y = MARGIN;

		this.addRenderableWidget(this.toggle(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.enabled",
				() -> config.enabled, v -> config.enabled = v));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.scale",
				0.25, 8.0, 0.25, () -> config.scale, v -> config.scale = (float) v, v -> String.format("%.2fx", v)));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.rotation",
				0, 355, 5, () -> config.rotation, v -> config.rotation = (int) v, v -> (int) v + "°"));
		y += step;

		this.addRenderableWidget(this.toggle(rx, y, half, h, "customcrosshair.option.flip_x",
				() -> config.flipX, v -> config.flipX = v));
		this.addRenderableWidget(this.toggle(rx + half + 2, y, half, h, "customcrosshair.option.flip_y",
				() -> config.flipY, v -> config.flipY = v));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.offset_x",
				-100, 100, 1, () -> config.offsetX, v -> config.offsetX = (int) v, v -> String.valueOf((int) v)));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.offset_y",
				-100, 100, 1, () -> config.offsetY, v -> config.offsetY = (int) v, v -> String.valueOf((int) v)));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.opacity",
				0, 1, 0.05, () -> config.opacity, v -> config.opacity = (float) v, v -> Math.round(v * 100) + "%"));
		y += step;

		this.addRenderableWidget(this.cycleButton(rx, y, COLUMN_WIDTH, h,
				() -> Component.translatable("customcrosshair.option.outline",
						Component.translatable("customcrosshair.outline." + config.outline)),
				() -> config.outline = (config.outline + 1) % 3));
		y += step;

		this.addRenderableWidget(this.toggle(rx, y, half, h, "customcrosshair.option.third_person",
				() -> config.showInThirdPerson, v -> config.showInThirdPerson = v));
		this.addRenderableWidget(this.toggle(rx + half + 2, y, half, h, "customcrosshair.option.debug",
				() -> config.showInDebug, v -> config.showInDebug = v));
		y += step;

		this.addRenderableWidget(this.toggle(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.attack_indicator",
				() -> config.attackIndicator, v -> config.attackIndicator = v));
		y += step;

		this.addRenderableWidget(Button.builder(Component.translatable("customcrosshair.option.reset_transform"), b -> {
			config.scale = 1.0F;
			config.rotation = 0;
			config.flipX = false;
			config.flipY = false;
			config.offsetX = 0;
			config.offsetY = 0;
			config.opacity = 1.0F;
			this.rebuild();
		}).bounds(rx, y, COLUMN_WIDTH, h).build());

		this.layoutGrid();
	}

	private void layoutGrid() {
		int left = MARGIN + COLUMN_WIDTH + 8;
		int right = this.width - MARGIN - COLUMN_WIDTH - 8;
		int top = MARGIN + 12;
		int bottom = this.height - MARGIN - 12 - PALETTE_SIZE - 6;
		int available = Math.min(right - left, bottom - top);

		this.cell = Math.max(2, available / config.gridSize);
		int gridPx = this.cell * config.gridSize;
		this.gridX = left + (right - left - gridPx) / 2;
		this.gridY = top + Math.max(0, (bottom - top - gridPx) / 2);
		this.paletteY = this.gridY + gridPx + 4;
	}

	private void rebuild() {
		this.clearWidgets();
		this.init();
	}

	private int colorSlider(int x, int y, int step, int h, String key, int shift) {
		int min = shift == 24 ? 16 : 0;
		this.addRenderableWidget(new ValueSlider(x, y, COLUMN_WIDTH, h, key, min, 255, 1,
				() -> (config.brushColor >>> shift) & 0xFF,
				v -> config.brushColor = (config.brushColor & ~(0xFF << shift)) | ((int) v << shift),
				v -> String.valueOf((int) v)));
		return y + step;
	}

	private Button toggle(int x, int y, int w, int h, String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		return this.cycleButton(x, y, w, h,
				() -> Component.translatable(key, CommonComponents.optionStatus(getter.get())),
				() -> setter.accept(!getter.get()));
	}

	private Button cycleButton(int x, int y, int w, int h, Supplier<Component> label, Runnable action) {
		return Button.builder(label.get(), b -> {
			action.run();
			b.setMessage(label.get());
		}).bounds(x, y, w, h).build();
	}

	// ---------------------------------------------------------------- rendering

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		graphics.centeredText(this.font, this.title.getString(), this.width / 2, MARGIN + 1, 0xFFFFFFFF);

		this.drawGrid(graphics, mouseX, mouseY);
		this.drawPalette(graphics, mouseX, mouseY);
		this.drawPreview(graphics);

		graphics.centeredText(this.font, Component.translatable("customcrosshair.hint").getString(),
				this.width / 2, this.height - MARGIN - 9, 0xFFB0B0B0);
	}

	private void drawGrid(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int size = config.gridSize;
		int px = size * this.cell;
		int center = size / 2;

		frame(graphics, this.gridX - 1, this.gridY - 1, px + 2, px + 2, 0xFF808080);

		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int sx = this.gridX + x * this.cell;
				int sy = this.gridY + y * this.cell;
				int background = ((x + y) & 1) == 0 ? 0xFF262626 : 0xFF303030;

				if (x == center || y == center) {
					background = ((x + y) & 1) == 0 ? 0xFF2E2E3A : 0xFF383844;
				}

				graphics.fill(sx, sy, sx + this.cell, sy + this.cell, background);

				int color = config.getPixel(x, y);

				if (color != 0) {
					graphics.fill(sx, sy, sx + this.cell, sy + this.cell, color);
				}
			}
		}

		int[] hovered = this.cellAt(mouseX, mouseY);

		if (hovered != null) {
			int sx = this.gridX + hovered[0] * this.cell;
			int sy = this.gridY + hovered[1] * this.cell;
			frame(graphics, sx, sy, this.cell, this.cell, 0xFFFFFF00);
		}
	}

	private void drawPalette(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int x = this.paletteStartX();

		// Current brush color, slightly bigger.
		frame(graphics, x - 1, this.paletteY - 1, PALETTE_SIZE * 2 + 2, PALETTE_SIZE + 2, 0xFFFFFFFF);
		graphics.fill(x, this.paletteY, x + PALETTE_SIZE * 2, this.paletteY + PALETTE_SIZE, config.brushColor);
		x += PALETTE_SIZE * 2 + 6;

		for (int color : PALETTE) {
			graphics.fill(x, this.paletteY, x + PALETTE_SIZE, this.paletteY + PALETTE_SIZE, color);

			if (inside(mouseX, mouseY, x, this.paletteY, PALETTE_SIZE, PALETTE_SIZE)) {
				frame(graphics, x - 1, this.paletteY - 1, PALETTE_SIZE + 2, PALETTE_SIZE + 2, 0xFFFFFF00);
			}

			x += PALETTE_SIZE + 2;
		}
	}

	private int paletteStartX() {
		int total = PALETTE_SIZE * 2 + 6 + PALETTE.length * (PALETTE_SIZE + 2);
		return this.width / 2 - total / 2;
	}

	private void drawPreview(GuiGraphicsExtractor graphics) {
		if (this.previewH < 24) return;

		graphics.fill(this.previewX, this.previewY, this.previewX + this.previewW, this.previewY + this.previewH, 0xFF6A8CAF);
		frame(graphics, this.previewX, this.previewY, this.previewW, this.previewH, 0xFF000000);
		graphics.text(this.font, Component.translatable("customcrosshair.preview").getString(),
				this.previewX + 3, this.previewY + 3, 0xFFFFFFFF);

		// Preview at the real in-game size (offset is ignored so it stays inside the box).
		graphics.enableScissor(this.previewX + 1, this.previewY + 1, this.previewX + this.previewW - 1, this.previewY + this.previewH - 1);
		CrosshairRenderer.draw(graphics, config, this.previewX + this.previewW / 2.0F, this.previewY + this.previewH / 2.0F + 4, config.scale);
		graphics.disableScissor();
	}

	private static void frame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
		graphics.fill(x, y, x + w, y + 1, color);
		graphics.fill(x, y + h - 1, x + w, y + h, color);
		graphics.fill(x, y, x + 1, y + h, color);
		graphics.fill(x + w - 1, y, x + w, y + h, color);
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	// ---------------------------------------------------------------- input

	private int[] cellAt(double mouseX, double mouseY) {
		int size = config.gridSize;

		if (!inside(mouseX, mouseY, this.gridX, this.gridY, size * this.cell, size * this.cell)) {
			return null;
		}

		return new int[] {(int) ((mouseX - this.gridX) / this.cell), (int) ((mouseY - this.gridY) / this.cell)};
	}

	private void paint(double mouseX, double mouseY, int button) {
		int[] pos = this.cellAt(mouseX, mouseY);
		if (pos == null) return;

		if (button == 2) {
			int picked = config.getPixel(pos[0], pos[1]);

			if (picked != 0) {
				config.brushColor = picked;
				this.rebuild();
			}

			return;
		}

		int color = button == 0 ? config.brushColor : 0;
		int x = pos[0];
		int y = pos[1];
		int mx = config.gridSize - 1 - x;
		int my = config.gridSize - 1 - y;

		config.setPixel(x, y, color);
		if ((config.mirrorMode & 1) != 0) config.setPixel(mx, y, color);
		if ((config.mirrorMode & 2) != 0) config.setPixel(x, my, color);
		if (config.mirrorMode == 3) config.setPixel(mx, my, color);
	}

	private boolean pickPalette(double mouseX, double mouseY) {
		int x = this.paletteStartX() + PALETTE_SIZE * 2 + 6;

		for (int color : PALETTE) {
			if (inside(mouseX, mouseY, x, this.paletteY, PALETTE_SIZE, PALETTE_SIZE)) {
				// Keep the alpha chosen with the slider.
				config.brushColor = (config.brushColor & 0xFF000000) | (color & 0xFFFFFF);
				this.rebuild();
				return true;
			}

			x += PALETTE_SIZE + 2;
		}

		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.cellAt(event.x(), event.y()) != null && event.button() <= 2) {
			this.paintButton = event.button();
			this.paint(event.x(), event.y(), event.button());
			return true;
		}

		if (event.button() == 0 && this.pickPalette(event.x(), event.y())) {
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (this.paintButton == 0 || this.paintButton == 1) {
			this.paint(event.x(), event.y(), this.paintButton);
			return true;
		}

		return super.mouseDragged(event, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.paintButton != -1) {
			this.paintButton = -1;
			return true;
		}

		return super.mouseReleased(event);
	}

	@Override
	public void removed() {
		config.save();
		super.removed();
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
