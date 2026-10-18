package dev.customcrosshair;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/**
 * All user settings. Stored as JSON in {@code config/customcrosshair.json}.
 */
public final class CrosshairConfig {
	public static final int MIN_GRID = 3;
	public static final int MAX_GRID = 31;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("customcrosshair.json");

	private static CrosshairConfig instance;

	/** When false the vanilla crosshair is used. */
	public boolean enabled = true;

	/** Side length of the square pixel grid (odd so that there is a center pixel). */
	public int gridSize = 15;
	/** ARGB colors, row by row; 0 means an empty pixel. */
	public int[] pixels = new int[15 * 15];

	/** Color used by the brush in the editor (ARGB). */
	public int brushColor = 0xFFFFFFFF;
	/** 0 = off, 1 = left/right, 2 = up/down, 3 = both. */
	public int mirrorMode = 3;

	/** Size of one crosshair pixel in GUI pixels. */
	public float scale = 1.0F;
	/** Rotation in degrees, clockwise. */
	public int rotation = 0;
	public boolean flipX = false;
	public boolean flipY = false;
	public int offsetX = 0;
	public int offsetY = 0;
	/** Global opacity multiplier, 0..1. */
	public float opacity = 1.0F;
	/** 0 = off, 1 = black, 2 = white. */
	public int outline = 0;

	/** Keep drawing the crosshair in third person (F5). */
	public boolean showInThirdPerson = true;
	/** Keep drawing this crosshair while the debug screen (F3) is open instead of the vanilla axes. */
	public boolean showInDebug = true;
	/** Show the attack cooldown bar under the crosshair. */
	public boolean attackIndicator = true;

	public static CrosshairConfig get() {
		if (instance == null) {
			instance = load();
		}

		return instance;
	}

	private static CrosshairConfig load() {
		CrosshairConfig config = null;

		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				config = GSON.fromJson(reader, CrosshairConfig.class);
			} catch (Exception e) {
				CustomCrosshairClient.LOGGER.error("Failed to read {}, using defaults", PATH, e);
			}
		}

		if (config == null) {
			config = new CrosshairConfig();
			CrosshairPresets.CROSS.apply(config);
		}

		config.validate();
		return config;
	}

	public void save() {
		validate();

		try {
			Files.createDirectories(PATH.getParent());

			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			CustomCrosshairClient.LOGGER.error("Failed to save {}", PATH, e);
		}
	}

	private void validate() {
		gridSize = clampGrid(gridSize);

		if (pixels == null || pixels.length != gridSize * gridSize) {
			int[] fixed = new int[gridSize * gridSize];

			if (pixels != null) {
				System.arraycopy(pixels, 0, fixed, 0, Math.min(pixels.length, fixed.length));
			}

			pixels = fixed;
		}

		mirrorMode = Math.floorMod(mirrorMode, 4);
		scale = Math.clamp(scale, 0.25F, 8.0F);
		rotation = Math.floorMod(rotation, 360);
		offsetX = Math.clamp(offsetX, -200, 200);
		offsetY = Math.clamp(offsetY, -200, 200);
		opacity = Math.clamp(opacity, 0.0F, 1.0F);
		outline = Math.floorMod(outline, 3);
	}

	public static int clampGrid(int size) {
		size = Math.clamp(size, MIN_GRID, MAX_GRID);
		return size % 2 == 0 ? size + 1 : size;
	}

	public int getPixel(int x, int y) {
		if (x < 0 || y < 0 || x >= gridSize || y >= gridSize) return 0;
		return pixels[y * gridSize + x];
	}

	public void setPixel(int x, int y, int color) {
		if (x < 0 || y < 0 || x >= gridSize || y >= gridSize) return;
		pixels[y * gridSize + x] = color;
	}

	/** Changes the grid size, keeping the drawing centered. */
	public void resize(int newSize) {
		newSize = clampGrid(newSize);
		if (newSize == gridSize) return;

		int[] resized = new int[newSize * newSize];
		int shift = (newSize - gridSize) / 2;

		for (int y = 0; y < gridSize; y++) {
			for (int x = 0; x < gridSize; x++) {
				int nx = x + shift;
				int ny = y + shift;

				if (nx >= 0 && ny >= 0 && nx < newSize && ny < newSize) {
					resized[ny * newSize + nx] = pixels[y * gridSize + x];
				}
			}
		}

		gridSize = newSize;
		pixels = resized;
	}

	public void clear() {
		pixels = new int[gridSize * gridSize];
	}

	/** Repaints every drawn pixel with the brush color. */
	public void recolor() {
		for (int i = 0; i < pixels.length; i++) {
			if (pixels[i] != 0) pixels[i] = brushColor;
		}
	}
}
