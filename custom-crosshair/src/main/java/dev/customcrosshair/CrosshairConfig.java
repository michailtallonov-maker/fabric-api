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
	// ---- Attack cooldown indicator ----

	/** Show the attack cooldown indicator. */
	public boolean attackIndicator = true;
	/** See {@link CooldownRenderer.Style}. */
	public int cdStyle = 0;
	public int cdBackgroundColor = 0xC0303030;
	public int cdFillColor = 0xFFFFFFFF;
	public int cdReadyColor = 0xFF55FF55;
	/** Fill color fades from {@link #cdFillColor} to {@link #cdReadyColor} while charging. */
	public boolean cdGradient = false;
	/** Bar length or ring diameter. */
	public int cdLength = 16;
	public int cdThickness = 2;
	/** 0 = below, 1 = above, 2 = left, 3 = right, 4 = center. */
	public int cdPosition = 0;
	public int cdOffsetX = 0;
	public int cdOffsetY = 0;
	/** Show the remaining time (draining) instead of the charge (filling). */
	public boolean cdReverse = false;
	/** Keep the indicator visible in {@link #cdReadyColor} when fully charged. */
	public boolean cdShowWhenReady = false;
	public boolean cdPercent = false;

	// ---- Hit particles ----

	/** Spawn custom particles when you hit an entity. */
	public boolean particlesEnabled = true;
	/** 0 = only critical / enchanted hits (replaces vanilla particles), 1 = every hit. */
	public int particleTrigger = 0;
	/** Hide the vanilla crit and enchanted-hit particles. */
	public boolean hideVanillaParticles = true;
	/** Index into {@link HitParticles#TYPES}. */
	public int particleType = 0;
	public int particleColor = 0xFFFF4040;
	public boolean particleTint = true;
	public boolean particleRainbow = false;
	public float particleSize = 1.0F;
	public int particleCount = 15;
	public float particleSpeed = 1.0F;
	public float particleGravity = 0.0F;
	/** Life time multiplier. */
	public float particleLifetime = 1.0F;

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
		cdStyle = Math.floorMod(cdStyle, CooldownRenderer.Style.values().length);
		cdLength = Math.clamp(cdLength, 2, 128);
		cdThickness = Math.clamp(cdThickness, 1, 32);
		cdPosition = Math.floorMod(cdPosition, 5);
		cdOffsetX = Math.clamp(cdOffsetX, -200, 200);
		cdOffsetY = Math.clamp(cdOffsetY, -200, 200);
		particleTrigger = Math.floorMod(particleTrigger, 2);
		particleType = Math.floorMod(particleType, HitParticles.TYPES.size());
		particleSize = Math.clamp(particleSize, 0.1F, 10.0F);
		particleCount = Math.clamp(particleCount, 1, 200);
		particleSpeed = Math.clamp(particleSpeed, 0.0F, 5.0F);
		particleGravity = Math.clamp(particleGravity, -2.0F, 2.0F);
		particleLifetime = Math.clamp(particleLifetime, 0.1F, 10.0F);
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
