package dev.customcrosshair.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import dev.customcrosshair.CrosshairConfig;
import dev.customcrosshair.CrosshairPresets;
import dev.customcrosshair.CrosshairRenderer;

/**
 * Pixel editor for the crosshair plus all display settings.
 *
 * <p>Left mouse button paints, right mouse button erases, middle mouse button picks a color.
 */
public class CrosshairEditorScreen extends BaseConfigScreen {
	private CrosshairPresets preset = CrosshairPresets.CROSS;

	// Layout, computed in init().
	private int gridX;
	private int gridY;
	private int cell;
	private int previewY;
	private int previewH;

	/** -1 when not painting, otherwise the mouse button that started the stroke. */
	private int paintButton = -1;

	public CrosshairEditorScreen(Screen parent) {
		super(parent, 0);
	}

	@Override
	protected int getEditColor() {
		return config.brushColor;
	}

	@Override
	protected void setEditColor(int color) {
		config.brushColor = color;
	}

	@Override
	protected void initTab() {
		int h = this.rowHeight;

		// ---- Left column: drawing ----
		int lx = this.leftX;
		int y = MARGIN;

		this.addRenderableWidget(new ValueSlider(lx, y, COLUMN_WIDTH, h, "customcrosshair.option.grid_size",
				CrosshairConfig.MIN_GRID, CrosshairConfig.MAX_GRID, 2, () -> config.gridSize,
				v -> {
					config.resize((int) v);
					this.layoutGrid();
				}, v -> (int) v + "x" + (int) v));
		y += step;

		y = this.colorSliders(lx, y);

		this.addRenderableWidget(this.choice(lx, y, COLUMN_WIDTH, "customcrosshair.option.mirror", "customcrosshair.mirror", 4,
				() -> config.mirrorMode, v -> config.mirrorMode = v));
		y += step;

		this.addRenderableWidget(this.cycleButton(lx, y, COLUMN_WIDTH,
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

		this.previewY = y + 2;
		this.previewH = Math.max(0, this.doneY() - 4 - this.previewY);

		// ---- Right column: display ----
		int rx = this.rightX;
		y = MARGIN;

		this.addRenderableWidget(this.toggle(rx, y, COLUMN_WIDTH, "customcrosshair.option.enabled",
				() -> config.enabled, v -> config.enabled = v));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.scale",
				0.25, 8.0, 0.25, () -> config.scale, v -> config.scale = (float) v, v -> String.format("%.2fx", v)));
		y += step;

		this.addRenderableWidget(new ValueSlider(rx, y, COLUMN_WIDTH, h, "customcrosshair.option.rotation",
				0, 355, 5, () -> config.rotation, v -> config.rotation = (int) v, v -> (int) v + "°"));
		y += step;

		this.addRenderableWidget(this.toggle(rx, y, half, "customcrosshair.option.flip_x",
				() -> config.flipX, v -> config.flipX = v));
		this.addRenderableWidget(this.toggle(rx + half + 2, y, half, "customcrosshair.option.flip_y",
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

		this.addRenderableWidget(this.choice(rx, y, COLUMN_WIDTH, "customcrosshair.option.outline", "customcrosshair.outline", 3,
				() -> config.outline, v -> config.outline = v));
		y += step;

		this.addRenderableWidget(this.toggle(rx, y, half, "customcrosshair.option.third_person",
				() -> config.showInThirdPerson, v -> config.showInThirdPerson = v));
		this.addRenderableWidget(this.toggle(rx + half + 2, y, half, "customcrosshair.option.debug",
				() -> config.showInDebug, v -> config.showInDebug = v));
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
		int left = this.centerLeft;
		int right = this.centerRight;
		int top = this.centerTop;
		int bottom = this.height - MARGIN - 12 - PALETTE_SIZE - 6;
		int available = Math.min(right - left, bottom - top);

		this.cell = Math.max(2, available / config.gridSize);
		int gridPx = this.cell * config.gridSize;
		this.gridX = left + (right - left - gridPx) / 2;
		this.gridY = top + Math.max(0, (bottom - top - gridPx) / 2);
		this.paletteY = this.gridY + gridPx + 4;
	}

	// ---------------------------------------------------------------- rendering

	@Override
	protected void extractTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		this.drawGrid(graphics, mouseX, mouseY);
		this.drawPreview(graphics);
	}

	@Override
	protected String hint() {
		return Component.translatable("customcrosshair.hint").getString();
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

	private void drawPreview(GuiGraphicsExtractor graphics) {
		if (this.previewH < 24) return;

		int x = this.leftX;
		int w = COLUMN_WIDTH;
		graphics.fill(x, this.previewY, x + w, this.previewY + this.previewH, 0xFF6A8CAF);
		frame(graphics, x, this.previewY, w, this.previewH, 0xFF000000);
		graphics.text(this.font, Component.translatable("customcrosshair.preview").getString(), x + 3, this.previewY + 3, 0xFFFFFFFF);

		// Preview at the real in-game size (offset is ignored so it stays inside the box).
		graphics.enableScissor(x + 1, this.previewY + 1, x + w - 1, this.previewY + this.previewH - 1);
		CrosshairRenderer.draw(graphics, config, x + w / 2.0F, this.previewY + this.previewH / 2.0F + 4, config.scale);
		graphics.disableScissor();
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

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.cellAt(event.x(), event.y()) != null && event.button() <= 2) {
			this.paintButton = event.button();
			this.paint(event.x(), event.y(), event.button());
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
}
