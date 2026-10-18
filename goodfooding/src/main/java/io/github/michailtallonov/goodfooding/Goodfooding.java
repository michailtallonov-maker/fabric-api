package io.github.michailtallonov.goodfooding;

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
 * Client-side goodfooding.
 *
 * <p>While enabled, the game's goodfooding entity is swapped for a private {@link Marker} that is never added to the level,
 * so the server knows nothing about it. The goodfooding has no collision and simply moves wherever the movement keys say,
 * which lets it pass through blocks. The real player stays where it was and receives no movement input.
 */
public final class Goodfooding {
	private static final Logger LOGGER = LoggerFactory.getLogger(GoodfoodingClient.MOD_ID);

	/** Speed in blocks per tick. */
	private static final double BASE_SPEED = 0.6;
	private static final double SPRINT_MULTIPLIER = 3.0;
	/** How quickly the goodfooding reaches its target velocity (0..1); lower values give a smoother glide. */
	private static final double ACCELERATION = 0.35;

	private static @Nullable Marker goodfooding;
	private static Vec3 velocity = Vec3.ZERO;

	private static @Nullable LocalPlayer savedPlayer;
	private static @Nullable ClientInput savedInput;
	private static @Nullable CameraType savedPerspective;
	private static @Nullable Boolean savedSmartCull;

	private Goodfooding() {
	}

	public static boolean isEnabled() {
		return goodfooding != null;
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

		Marker goodfoodingEntity = new Marker(EntityTypes.MARKER, client.level);
		Vec3 eye = player.getEyePosition();
		goodfoodingEntity.setPos(eye.x, eye.y - goodfoodingEntity.getEyeHeight(), eye.z);
		goodfoodingEntity.setYRot(player.getYRot());
		goodfoodingEntity.setXRot(player.getXRot());
		goodfoodingEntity.yRotO = goodfoodingEntity.getYRot();
		goodfoodingEntity.xRotO = goodfoodingEntity.getXRot();
		goodfoodingEntity.xo = goodfoodingEntity.getX();
		goodfoodingEntity.yo = goodfoodingEntity.getY();
		goodfoodingEntity.zo = goodfoodingEntity.getZ();
		velocity = Vec3.ZERO;

		// A bare ClientInput never reports any pressed keys, so the player stands still while the keys drive the goodfooding.
		savedPlayer = player;
		savedInput = player.input;
		player.input = new ClientInput();

		savedPerspective = client.options.getCameraType();
		client.options.setCameraType(CameraType.FIRST_PERSON);

		// Without this, sections around the goodfooding get culled away while it is inside solid blocks.
		savedSmartCull = setSmartCull(client, false);

		goodfooding = goodfoodingEntity;
		client.setCameraEntity(goodfoodingEntity);
		client.gui.hud.setOverlayMessage(Component.translatable("goodfooding.enabled"), false);
	}

	public static void disable(Minecraft client, boolean notify) {
		Marker goodfoodingEntity = goodfooding;

		if (goodfoodingEntity == null) {
			return;
		}

		goodfooding = null;
		velocity = Vec3.ZERO;

		LocalPlayer player = client.player;

		// Only give the input back to the player it was taken from; after a respawn the new player has its own.
		if (player != null && player == savedPlayer && savedInput != null) {
			player.input = savedInput;
		}

		if (client.getCameraEntity() == goodfoodingEntity) {
			client.setCameraEntity(player);
		}

		if (savedPerspective != null) {
			client.options.setCameraType(savedPerspective);
		}

		if (savedSmartCull != null) {
			setSmartCull(client, savedSmartCull);
		}

		savedPlayer = null;
		savedInput = null;
		savedPerspective = null;
		savedSmartCull = null;

		if (notify && player != null) {
			client.gui.hud.setOverlayMessage(Component.translatable("goodfooding.disabled"), false);
		}
	}

	/**
	 * Applies mouse movement to the goodfooding instead of the player.
	 */
	public static void turn(double yRot, double xRot) {
		if (goodfooding != null) {
			goodfooding.turn(yRot, xRot);
		}
	}

	static void tick(Minecraft client) {
		Marker goodfoodingEntity = goodfooding;

		if (goodfoodingEntity == null) {
			return;
		}

		// Something else took over the goodfooding (respawn, dimension change, spectating via the server...).
		if (client.player == null || client.level == null || client.getCameraEntity() != goodfoodingEntity || goodfoodingEntity.level() != client.level) {
			disable(client, false);
			return;
		}

		blockInteraction(client.options);
		move(goodfoodingEntity, client.options);
	}

	/**
	 * Interacting would target whatever the goodfooding is looking at, which is far from the player.
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

	private static void move(Marker goodfoodingEntity, Options options) {
		double forward = axis(options.keyUp, options.keyDown);
		double strafe = axis(options.keyLeft, options.keyRight);
		double vertical = axis(options.keyJump, options.keyShift);

		double yaw = Math.toRadians(goodfoodingEntity.getYRot());
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
		goodfoodingEntity.xo = goodfoodingEntity.getX();
		goodfoodingEntity.yo = goodfoodingEntity.getY();
		goodfoodingEntity.zo = goodfoodingEntity.getZ();
		goodfoodingEntity.setPos(goodfoodingEntity.getX() + velocity.x, goodfoodingEntity.getY() + velocity.y, goodfoodingEntity.getZ() + velocity.z);
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
