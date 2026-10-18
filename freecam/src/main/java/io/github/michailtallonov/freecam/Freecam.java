package io.github.michailtallonov.freecam;

import java.lang.reflect.Field;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side free camera.
 *
 * <p>While enabled, the game's camera entity is swapped for a private {@link Marker} that is never added to the level,
 * so the server knows nothing about it. The camera has no collision and simply moves wherever the movement keys say,
 * which lets it pass through blocks. The real player stays where it was and receives no movement input.
 */
public final class Freecam {
	private static final Logger LOGGER = LoggerFactory.getLogger(FreecamClient.MOD_ID);

	/** Speed in blocks per tick. */
	private static final double BASE_SPEED = 0.6;
	private static final double SPRINT_MULTIPLIER = 3.0;
	/** How quickly the camera reaches its target velocity (0..1); lower values give a smoother glide. */
	private static final double ACCELERATION = 0.35;

	private static @Nullable Marker camera;
	private static Vec3 velocity = Vec3.ZERO;

	private static @Nullable LocalPlayer savedPlayer;
	private static @Nullable ClientInput savedInput;
	private static @Nullable CameraType savedCameraType;
	private static @Nullable Boolean savedSmartCull;

	private Freecam() {
	}

	public static boolean isEnabled() {
		return camera != null;
	}

	public static void toggle(Minecraft client) {
		if (isEnabled()) {
			disable(client, true);
		} else {
			enable(client);
		}
	}

	public static void enable(Minecraft client) {
		LocalPlayer player = client.player;

		if (isEnabled() || player == null || client.level == null) {
			return;
		}

		Marker cam = new Marker(EntityTypes.MARKER, client.level);
		Vec3 eye = player.getEyePosition();
		cam.setPos(eye.x, eye.y - cam.getEyeHeight(), eye.z);
		cam.setYRot(player.getYRot());
		cam.setXRot(player.getXRot());
		cam.yRotO = cam.getYRot();
		cam.xRotO = cam.getXRot();
		cam.xo = cam.getX();
		cam.yo = cam.getY();
		cam.zo = cam.getZ();
		velocity = Vec3.ZERO;

		// A bare ClientInput never reports any pressed keys, so the player stands still while the keys drive the camera.
		savedPlayer = player;
		savedInput = player.input;
		player.input = new ClientInput();

		savedCameraType = client.options.getCameraType();
		client.options.setCameraType(CameraType.FIRST_PERSON);

		// Without this, sections around the camera get culled away while it is inside solid blocks.
		savedSmartCull = setSmartCull(client, false);

		camera = cam;
		client.setCameraEntity(cam);
		client.gui.hud.setOverlayMessage(Component.translatable("freecam.enabled"), false);
	}

	public static void disable(Minecraft client, boolean notify) {
		Marker cam = camera;

		if (cam == null) {
			return;
		}

		camera = null;
		velocity = Vec3.ZERO;

		LocalPlayer player = client.player;

		// Only give the input back to the player it was taken from; after a respawn the new player has its own.
		if (player != null && player == savedPlayer && savedInput != null) {
			player.input = savedInput;
		}

		if (client.getCameraEntity() == cam) {
			client.setCameraEntity(player);
		}

		if (savedCameraType != null) {
			client.options.setCameraType(savedCameraType);
		}

		if (savedSmartCull != null) {
			setSmartCull(client, savedSmartCull);
		}

		savedPlayer = null;
		savedInput = null;
		savedCameraType = null;
		savedSmartCull = null;

		if (notify && player != null) {
			client.gui.hud.setOverlayMessage(Component.translatable("freecam.disabled"), false);
		}
	}

	/**
	 * Applies mouse movement to the camera instead of the player.
	 */
	public static void turn(double yRot, double xRot) {
		if (camera != null) {
			camera.turn(yRot, xRot);
		}
	}

	static void tick(Minecraft client) {
		Marker cam = camera;

		if (cam == null) {
			return;
		}

		// Something else took over the camera (respawn, dimension change, spectating via the server...).
		if (client.player == null || client.level == null || client.getCameraEntity() != cam || cam.level() != client.level) {
			disable(client, false);
			return;
		}

		blockInteraction(client.options);
		move(cam, client.options);
	}

	/**
	 * Interacting would target whatever the free camera is looking at, which is far from the player.
	 * Swallow those inputs so nothing gets sent to the server that the player could not have done.
	 */
	private static void blockInteraction(Options options) {
		for (KeyMapping key : new KeyMapping[] {options.keyAttack, options.keyUse, options.keyPickItem}) {
			while (key.consumeClick()) {
				// discard
			}

			key.setDown(false);
		}
	}

	private static void move(Marker cam, Options options) {
		double forward = axis(options.keyUp, options.keyDown);
		double strafe = axis(options.keyLeft, options.keyRight);
		double vertical = axis(options.keyJump, options.keyShift);

		double yaw = Math.toRadians(cam.getYRot());
		double sin = Math.sin(yaw);
		double cos = Math.cos(yaw);

		Vec3 target = new Vec3(strafe * cos - forward * sin, vertical, forward * cos + strafe * sin);

		if (target.lengthSqr() > 1.0) {
			target = target.normalize();
		}

		double speed = BASE_SPEED * (options.keySprint.isDown() ? SPRINT_MULTIPLIER : 1.0);
		velocity = velocity.lerp(target.scale(speed), ACCELERATION);

		if (velocity.lengthSqr() < 1.0E-6) {
			velocity = Vec3.ZERO;
		}

		// Store the previous position so the renderer interpolates smoothly between ticks.
		cam.xo = cam.getX();
		cam.yo = cam.getY();
		cam.zo = cam.getZ();
		cam.setPos(cam.getX() + velocity.x, cam.getY() + velocity.y, cam.getZ() + velocity.z);
	}

	private static double axis(KeyMapping positive, KeyMapping negative) {
		return (positive.isDown() ? 1.0 : 0.0) - (negative.isDown() ? 1.0 : 0.0);
	}

	/**
	 * Sets {@code Minecraft.smartCull} if the field exists.
	 *
	 * @return the previous value, or {@code null} if the field could not be accessed
	 */
	private static @Nullable Boolean setSmartCull(Minecraft client, boolean value) {
		try {
			Field field = Minecraft.class.getDeclaredField("smartCull");
			field.setAccessible(true);
			boolean previous = field.getBoolean(client);
			field.setBoolean(client, value);
			return previous;
		} catch (ReflectiveOperationException | RuntimeException e) {
			LOGGER.debug("Could not toggle smart culling", e);
			return null;
		}
	}
}
