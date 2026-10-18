package dev.customcrosshair.screen;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/**
 * Slider over a numeric range with a fixed step.
 */
public class ValueSlider extends AbstractSliderButton {
	private final String key;
	private final double min;
	private final double max;
	private final double step;
	private final DoubleConsumer setter;
	private final DoubleFunction<String> format;

	public ValueSlider(int x, int y, int width, int height, String key, double min, double max, double step,
			DoubleSupplier getter, DoubleConsumer setter, DoubleFunction<String> format) {
		super(x, y, width, height, Component.empty(), (getter.getAsDouble() - min) / (max - min));
		this.key = key;
		this.min = min;
		this.max = max;
		this.step = step;
		this.setter = setter;
		this.format = format;
		this.updateMessage();
	}

	public double current() {
		double raw = this.min + this.value * (this.max - this.min);
		double snapped = this.min + Math.round((raw - this.min) / this.step) * this.step;
		return Math.clamp(snapped, this.min, this.max);
	}

	@Override
	protected void updateMessage() {
		this.setMessage(Component.translatable(this.key, this.format.apply(this.current())));
	}

	@Override
	protected void applyValue() {
		this.setter.accept(this.current());
	}
}
