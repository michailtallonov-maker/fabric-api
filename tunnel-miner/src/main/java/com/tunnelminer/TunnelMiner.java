package com.tunnelminer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;

/**
 * Client-side 3x3 tunnel miner.
 *
 * <p>When the player breaks a block with a pickaxe, the other 8 blocks of the 3x3 square around it are broken too.
 * The square faces the direction the player is looking (a wall when looking forward, a floor or ceiling when
 * looking steeply down or up).
 *
 * <ul>
 * <li>In singleplayer the blocks are broken instantly on the integrated server, exactly as if the player broke each
 * of them: drops, statistics and one point of pickaxe durability per block.</li>
 * <li>On a remote server instant breaking would be rejected, so the blocks are mined one after another through the
 * normal client mining logic, with real mining time for each block.</li>
 * </ul>
 */
public final class TunnelMiner implements ClientModInitializer {
	/** Stop when the tool has this much durability left, so the pickaxe never breaks. */
	private static final int MIN_DURABILITY_LEFT = 2;
	/** Looking further up or down than this (degrees) mines a horizontal 3x3 instead of a wall. */
	private static final float VERTICAL_PITCH = 50.0F;

	private static KeyMapping toggleKey;
	private static boolean enabled = false;

	private static Job job;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("tunnelminer", "main"));
		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.tunnelminer.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, category));

		ClientPlayerBlockBreakEvents.AFTER.register(TunnelMiner::onBlockBroken);
		ClientTickEvents.END_CLIENT_TICK.register(TunnelMiner::onEndTick);
	}

	/** Whether blocks are currently being mined on a remote server; used to suppress vanilla attack handling. */
	public static boolean isMining() {
		return job != null;
	}

	private static void onBlockBroken(ClientLevel level, LocalPlayer player, BlockPos pos, BlockState state) {
		if (!enabled || player.isSpectator()) return;

		// Blocks broken by the running job itself.
		if (job != null && job.blocks.contains(pos)) return;

		ItemStack tool = player.getMainHandItem();
		if (!tool.is(ItemTags.PICKAXES) || !canBreak(level, pos, state, tool)) return;

		List<BlockPos> targets = square(player, pos.immutable());
		Minecraft client = Minecraft.getInstance();
		IntegratedServer server = client.getSingleplayerServer();

		if (server != null) {
			breakInstantly(client, server, player.getUUID(), targets);
		} else {
			Job created = new Job(level);
			created.queue.addAll(targets);
			created.blocks.addAll(targets);
			job = created;
		}
	}

	/** The 8 blocks around {@code center} in the plane facing the player. */
	private static List<BlockPos> square(LocalPlayer player, BlockPos center) {
		Direction.Axis axis = Math.abs(player.getXRot()) > VERTICAL_PITCH ? Direction.Axis.Y : player.getDirection().getAxis();
		List<BlockPos> result = new ArrayList<>(8);

		for (int a = -1; a <= 1; a++) {
			for (int b = -1; b <= 1; b++) {
				if (a == 0 && b == 0) continue;

				result.add(switch (axis) {
				case X -> center.offset(0, a, b);
				case Y -> center.offset(a, 0, b);
				case Z -> center.offset(a, b, 0);
				});
			}
		}

		return result;
	}

	/** Singleplayer: break every block on the server thread, as if the player broke it. */
	private static void breakInstantly(Minecraft client, IntegratedServer server, UUID playerId, List<BlockPos> targets) {
		server.execute(() -> {
			ServerPlayer serverPlayer = server.getPlayerList().getPlayer(playerId);
			if (serverPlayer == null) return;

			for (BlockPos pos : targets) {
				ItemStack tool = serverPlayer.getMainHandItem();
				if (!tool.is(ItemTags.PICKAXES)) return;

				if (isToolLow(tool)) {
					client.execute(() -> {
						if (client.player != null) {
							client.player.sendOverlayMessage(Component.translatable("tunnelminer.tool_low"));
						}
					});
					return;
				}

				if (canBreak(serverPlayer.level(), pos, serverPlayer.level().getBlockState(pos), tool)) {
					serverPlayer.gameMode.destroyBlock(pos);
				}
			}
		});
	}

	private static void onEndTick(Minecraft client) {
		while (toggleKey.consumeClick()) {
			enabled = !enabled;
			cancel(client);

			if (client.player != null) {
				client.player.sendOverlayMessage(Component.translatable(enabled ? "tunnelminer.enabled" : "tunnelminer.disabled"));
			}
		}

		if (job != null) {
			tickJob(client);
		}
	}

	/** Remote server: mine the queued blocks one by one with the normal mining logic. */
	private static void tickJob(Minecraft client) {
		LocalPlayer player = client.player;
		ClientLevel level = client.level;

		if (player == null || level == null || client.gameMode == null || level != job.level || !player.isAlive() || !client.mouseHandler.isMouseGrabbed()) {
			cancel(client);
			return;
		}

		ItemStack tool = player.getMainHandItem();

		if (!tool.is(ItemTags.PICKAXES)) {
			cancel(client);
			return;
		}

		if (isToolLow(tool)) {
			player.sendOverlayMessage(Component.translatable("tunnelminer.tool_low"));
			cancel(client);
			return;
		}

		// Drop the current target if it was broken (or changed) or went out of reach.
		if (job.current != null && !canMine(level, player, job.current, tool)) {
			job.current = null;
		}

		while (job.current == null) {
			BlockPos next = job.queue.poll();

			if (next == null) {
				job = null;
				return;
			}

			if (canMine(level, player, next, tool)) {
				job.current = next;
			}
		}

		client.gameMode.continueDestroyBlock(job.current, faceTowardsPlayer(player, job.current));
		player.swing(InteractionHand.MAIN_HAND);
	}

	private static boolean canMine(ClientLevel level, LocalPlayer player, BlockPos pos, ItemStack tool) {
		return canBreak(level, pos, level.getBlockState(pos), tool) && player.isWithinBlockInteractionRange(pos, 0.0);
	}

	/**
	 * Only stone-like and dirt-like blocks are broken: no air, fluids, unbreakable blocks or blocks with a block
	 * entity (chests, furnaces, spawners...), and no blocks that would drop nothing with this pickaxe.
	 */
	private static boolean canBreak(BlockGetter level, BlockPos pos, BlockState state, ItemStack tool) {
		if (state.isAir() || state.hasBlockEntity()) return false;
		if (state.getDestroySpeed(level, pos) < 0) return false;
		if (!state.is(BlockTags.MINEABLE_WITH_PICKAXE) && !state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return false;

		return !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
	}

	private static boolean isToolLow(ItemStack tool) {
		return tool.isDamageableItem() && tool.getMaxDamage() - tool.getDamageValue() <= MIN_DURABILITY_LEFT;
	}

	private static void cancel(Minecraft client) {
		if (job != null && job.current != null && client.gameMode != null) {
			client.gameMode.stopDestroyBlock();
		}

		job = null;
	}

	private static Direction faceTowardsPlayer(LocalPlayer player, BlockPos pos) {
		Vec3 eye = player.getEyePosition();
		double dx = eye.x - (pos.getX() + 0.5);
		double dy = eye.y - (pos.getY() + 0.5);
		double dz = eye.z - (pos.getZ() + 0.5);
		double ax = Math.abs(dx);
		double ay = Math.abs(dy);
		double az = Math.abs(dz);

		if (ay >= ax && ay >= az) return dy > 0 ? Direction.UP : Direction.DOWN;
		if (ax >= az) return dx > 0 ? Direction.EAST : Direction.WEST;
		return dz > 0 ? Direction.SOUTH : Direction.NORTH;
	}

	private static final class Job {
		final ClientLevel level;
		final Deque<BlockPos> queue = new ArrayDeque<>();
		final Set<BlockPos> blocks = new HashSet<>();
		BlockPos current;

		Job(ClientLevel level) {
			this.level = level;
		}
	}
}
