package dev.customcrosshair;

/**
 * Ready-made shapes that can be used as a starting point in the editor.
 * Every preset is drawn with the current brush color and fits any grid size.
 */
public enum CrosshairPresets {
	CROSS {
		@Override
		boolean filled(int dx, int dy, int r) {
			return (dx == 0 && Math.abs(dy) <= r) || (dy == 0 && Math.abs(dx) <= r);
		}
	},
	GAP_CROSS {
		@Override
		boolean filled(int dx, int dy, int r) {
			int gap = Math.max(1, r / 3);
			return (dx == 0 && Math.abs(dy) > gap && Math.abs(dy) <= r)
					|| (dy == 0 && Math.abs(dx) > gap && Math.abs(dx) <= r)
					|| (dx == 0 && dy == 0);
		}
	},
	DOT {
		@Override
		boolean filled(int dx, int dy, int r) {
			return Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && !(Math.abs(dx) == 1 && Math.abs(dy) == 1);
		}
	},
	CIRCLE {
		@Override
		boolean filled(int dx, int dy, int r) {
			double d = Math.sqrt(dx * dx + dy * dy);
			return Math.abs(d - r) < 0.5 || (dx == 0 && dy == 0);
		}
	},
	X {
		@Override
		boolean filled(int dx, int dy, int r) {
			return Math.abs(dx) == Math.abs(dy) && Math.abs(dx) <= r;
		}
	},
	T {
		@Override
		boolean filled(int dx, int dy, int r) {
			return (dx == 0 && dy >= 0 && dy <= r) || (dy == 0 && Math.abs(dx) <= r);
		}
	},
	SQUARE {
		@Override
		boolean filled(int dx, int dy, int r) {
			int s = Math.max(1, r / 2);
			return (Math.max(Math.abs(dx), Math.abs(dy)) == s) || (dx == 0 && dy == 0);
		}
	};

	abstract boolean filled(int dx, int dy, int r);

	public String translationKey() {
		return "customcrosshair.preset." + name().toLowerCase(java.util.Locale.ROOT);
	}

	public CrosshairPresets next() {
		CrosshairPresets[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public void apply(CrosshairConfig config) {
		config.clear();
		int size = config.gridSize;
		int center = size / 2;
		int radius = Math.min(center, 5);

		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				if (filled(x - center, y - center, radius)) {
					config.setPixel(x, y, config.brushColor);
				}
			}
		}
	}
}
