package dev.customcrosshair.screen;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import dev.customcrosshair.CrosshairConfig;

/**
 * Common parts of the three settings tabs: tab row, two side columns, color sliders and palette.
 */
public abstract class BaseConfigScreen extends Screen {
	protected static final int MARGIN = 6;
	protected static final int COLUMN_WIDTH = 124;
	protected static final int PALETTE_SIZE = 10;
	protected static final int[] PALETTE = {
			0xFFFFFFFF, 0xFFA0A0A0, 0xFF000000, 0xFFFF3030, 0xFFFF9020, 0xFFFFE030,
			0xFF60FF40, 0xFF20A040, 0xFF30E0FF, 0xFF3060FF, 0xFFA040FF, 0xFFFF40C0,
	};
	private static final String[] TABS = {"crosshair", "cooldown", "particles"};
	/** Tab that was open last, reopened by the key binding. */
	private static int lastTab = 0;

	protected final Screen parent;
	protected final CrosshairConfig config = CrosshairConfig.get();
	private final int tab;

	// Layout shared by all tabs, computed in init().
	protected int step;
	protected int rowHeight;
	protected int half;
	protected int leftX;
	protected int rightX;
	/** Free area between the two columns. */
	protected int centerLeft;
	protected int centerRight;
	protected int centerTop;
	protected int paletteY;

	protected BaseConfigScreen(Screen parent, int tab) {
		super(Component.translatable("customcrosshair.title"));
		this.parent = parent;
		this.tab = tab;
		lastTab = tab;
	}

	public static Screen openLast(Screen parent) {
		return open(parent, lastTab);
	}

	public static Screen open(Screen parent, int tab) {
		return switch (tab) {
			case 1 -> new CooldownScreen(parent);
			case 2 -> new ParticlesScreen(parent);
			default -> new CrosshairEditorScreen(parent);
		};
	}

	@Override
	protected void init() {
		this.step = Math.clamp((this.height - MARGIN * 2) / 11, 14, 22);
		this.rowHeight = this.step - 2;
		this.half = (COLUMN_WIDTH - 2) / 2;
		this.leftX = MARGIN;
		this.rightX = this.width - MARGIN - COLUMN_WIDTH;
		this.centerLeft = MARGIN + COLUMN_WIDTH + 8;
		this.centerRight = this.width - MARGIN - COLUMN_WIDTH - 8;

		int tabWidth = Math.min(80, (this.centerRight - this.centerLeft - 4) / TABS.length);
		int tabX = (this.centerLeft + this.centerRight) / 2 - (tabWidth * TABS.length + 2 * (TABS.length - 1)) / 2;

		for (int i = 0; i < TABS.length; i++) {
			int target = i;
			Button button = Button.builder(Component.translatable("customcrosshair.tab." + TABS[i]), b -> this.switchTab(target))
					.bounds(tabX + i * (tabWidth + 2), MARGIN, tabWidth, 14).build();
			button.active = i != this.tab;
			this.addRenderableWidget(button);
		}

		this.centerTop = MARGIN + 18;

		int doneY = this.height - MARGIN - this.rowHeight;
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
				.bounds(this.leftX, doneY, COLUMN_WIDTH, this.rowHeight).build());

		this.initTab();
	}

	protected abstract void initTab();

	/** Color edited by the RGBA sliders and the palette on this tab. */
	protected abstract int getEditColor();

	protected abstract void setEditColor(int color);

	/** Y of the "Done" button, the bottom of the free space in the left column. */
	protected int doneY() {
		return this.height - MARGIN - this.rowHeight;
	}

	private void switchTab(int target) {
		this.config.save();
		this.minecraft.gui.setScreen(open(this.parent, target));
	}

	protected void rebuild() {
		this.clearWidgets();
		this.init();
	}

	// ---------------------------------------------------------------- widgets

	/** Adds R, G, B and alpha sliders for {@link #getEditColor()}; returns the next free y. */
	protected int colorSliders(int x, int y) {
		y = this.colorSlider(x, y, "customcrosshair.option.red", 16);
		y = this.colorSlider(x, y, "customcrosshair.option.green", 8);
		y = this.colorSlider(x, y, "customcrosshair.option.blue", 0);
		return this.colorSlider(x, y, "customcrosshair.option.alpha", 24);
	}

	private int colorSlider(int x, int y, String key, int shift) {
		int min = shift == 24 ? 16 : 0;
		this.addRenderableWidget(new ValueSlider(x, y, COLUMN_WIDTH, this.rowHeight, key, min, 255, 1,
				() -> (this.getEditColor() >>> shift) & 0xFF,
				v -> this.setEditColor((this.getEditColor() & ~(0xFF << shift)) | ((int) v << shift)),
				v -> String.valueOf((int) v)));
		return y + this.step;
	}

	protected Button toggle(int x, int y, int w, String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		return this.cycleButton(x, y, w,
				() -> Component.translatable(key, CommonComponents.optionStatus(getter.get())),
				() -> setter.accept(!getter.get()));
	}

	/** Button that cycles an int option through {@code count} values, labelled {@code <valueKey>.<value>}. */
	protected Button choice(int x, int y, int w, String key, String valueKey, int count,
			Supplier<Integer> getter, Consumer<Integer> setter) {
		return this.choice(x, y, w, key, count, getter, setter, v -> Component.translatable(valueKey + "." + v));
	}

	protected Button choice(int x, int y, int w, String key, int count, Supplier<Integer> getter, Consumer<Integer> setter,
			Function<Integer, Component> valueName) {
		return this.cycleButton(x, y, w,
				() -> Component.translatable(key, valueName.apply(getter.get())),
				() -> setter.accept((getter.get() + 1) % count));
	}

	protected Button cycleButton(int x, int y, int w, Supplier<Component> label, Runnable action) {
		return Button.builder(label.get(), b -> {
			action.run();
			b.setMessage(label.get());
		}).bounds(x, y, w, this.rowHeight).build();
	}

	// ---------------------------------------------------------------- rendering

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		this.extractTab(graphics, mouseX, mouseY, delta);
		this.drawPalette(graphics, mouseX, mouseY);

		String hint = this.hint();

		if (hint != null) {
			graphics.centeredText(this.font, hint, (this.centerLeft + this.centerRight) / 2, this.height - MARGIN - 9, 0xFFB0B0B0);
		}
	}

	protected abstract void extractTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta);

	protected String hint() {
		return null;
	}

	private void drawPalette(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int x = this.paletteStartX();

		// Currently edited color, slightly bigger.
		frame(graphics, x - 1, this.paletteY - 1, PALETTE_SIZE * 2 + 2, PALETTE_SIZE + 2, 0xFFFFFFFF);
		graphics.fill(x, this.paletteY, x + PALETTE_SIZE * 2, this.paletteY + PALETTE_SIZE, this.getEditColor());
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
		return (this.centerLeft + this.centerRight) / 2 - total / 2;
	}

	private boolean pickPalette(double mouseX, double mouseY) {
		int x = this.paletteStartX() + PALETTE_SIZE * 2 + 6;

		for (int color : PALETTE) {
			if (inside(mouseX, mouseY, x, this.paletteY, PALETTE_SIZE, PALETTE_SIZE)) {
				// Keep the alpha chosen with the slider.
				this.setEditColor((this.getEditColor() & 0xFF000000) | (color & 0xFFFFFF));
				this.rebuild();
				return true;
			}

			x += PALETTE_SIZE + 2;
		}

		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0 && this.pickPalette(event.x(), event.y())) {
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	protected static void frame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
		graphics.fill(x, y, x + w, y + 1, color);
		graphics.fill(x, y + h - 1, x + w, y + h, color);
		graphics.fill(x, y, x + 1, y + h, color);
		graphics.fill(x + w - 1, y, x + w, y + h, color);
	}

	protected static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	@Override
	public void removed() {
		this.config.save();
		super.removed();
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
