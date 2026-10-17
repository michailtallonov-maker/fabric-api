package com.oreveinminer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;

/**
 * Client-side vein miner.
 *
 * <p>When the player breaks an ore block with a pickaxe, every connected ore block of the same kind is
 * queued and then mined one by one through the normal client mining logic
 * ({@code MultiPlayerGameMode#continueDestroyBlock}). The server therefore sees ordinary block breaking
 * packets with real mining time, so this works both in singleplayer and on servers without any server-side mod.
 */
public final class OreVeinMiner implements ClientModInitializer {
	/** Maximum number of extra blocks mined per vein. */
	private static final int MAX_BLOCKS = 64;
	/** Maximum distance (per axis) from the first broken block that the vein search may reach. */
	private static final int MAX_RADIUS = 8;
	/** Stop when the tool has this much durability left, so the pickaxe never breaks. */
	private static final int MIN_DURABILITY_LEFT = 2;

	private static KeyMapping toggleKey;
	private static boolean enabled = true;

	private static Job job;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("oreveinminer", "main"));
		toggleKey = KeyMappingHelper.registerKeyMapping(Compat.toggleKey("key.oreveinminer.toggle", category));

		ClientPlayerBlockBreakEvents.AFTER.register(OreVeinMiner::onBlockBroken);
		ClientTickEvents.END_CLIENT_TICK.register(OreVeinMiner::onEndTick);
	}

	/** Whether a vein is currently being mined; used to suppress vanilla attack handling meanwhile. */
	public static boolean isMining() {
		return job != null;
	}

	private static void onBlockBroken(ClientLevel level, LocalPlayer player, BlockPos pos, BlockState state) {
		if (!enabled || player.isSpectator()) return;

		// Blocks broken by the running job itself.
		if (job != null && job.blocks.contains(pos)) return;

		String family = oreFamily(state);
		if (family == null) return;

		ItemStack tool = player.getMainHandItem();
		if (!tool.is(ItemTags.PICKAXES) || !tool.isCorrectToolForDrops(state)) return;

		Job found = findVein(level, pos.immutable(), family);
		if (!found.queue.isEmpty()) {
			job = found;
		}
	}

	private static void onEndTick(Minecraft client) {
		while (toggleKey.consumeClick()) {
			enabled = !enabled;
			cancel(client);

			if (client.player != null) {
				client.player.sendOverlayMessage(Component.translatable(enabled ? "oreveinminer.enabled" : "oreveinminer.disabled"));
			}
		}

		if (job != null) {
			tickJob(client);
		}
	}

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

		if (tool.isDamageableItem() && tool.getMaxDamage() - tool.getDamageValue() <= MIN_DURABILITY_LEFT) {
			player.sendOverlayMessage(Component.translatable("oreveinminer.tool_low"));
			cancel(client);
			return;
		}

		// Drop the current target if it was broken (or changed) or went out of reach.
		if (job.current != null && !canMine(level, player, job.current, job.family)) {
			job.current = null;
		}

		while (job.current == null) {
			BlockPos next = job.queue.poll();

			if (next == null) {
				job = null;
				return;
			}

			if (canMine(level, player, next, job.family)) {
				job.current = next;
			}
		}

		client.gameMode.continueDestroyBlock(job.current, faceTowardsPlayer(player, job.current));
		Compat.swingMainHand(player);
	}

	private static boolean canMine(ClientLevel level, LocalPlayer player, BlockPos pos, String family) {
		return family.equals(oreFamily(level.getBlockState(pos))) && player.isWithinBlockInteractionRange(pos, 0.0);
	}

	private static void cancel(Minecraft client) {
		if (job != null && job.current != null && client.gameMode != null) {
			client.gameMode.stopDestroyBlock();
		}

		job = null;
	}

	/** Breadth-first search over all 26 neighbours, so diagonally connected ore counts as one vein too. */
	private static Job findVein(ClientLevel level, BlockPos origin, String family) {
		Job result = new Job(level, family);
		Deque<BlockPos> open = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();
		open.add(origin);
		visited.add(origin);

		while (!open.isEmpty() && result.queue.size() < MAX_BLOCKS) {
			BlockPos pos = open.poll();

			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) continue;

						BlockPos neighbour = pos.offset(dx, dy, dz);

						if (Math.abs(neighbour.getX() - origin.getX()) > MAX_RADIUS
								|| Math.abs(neighbour.getY() - origin.getY()) > MAX_RADIUS
								|| Math.abs(neighbour.getZ() - origin.getZ()) > MAX_RADIUS) {
							continue;
						}

						if (!visited.add(neighbour)) continue;
						if (!family.equals(oreFamily(level.getBlockState(neighbour)))) continue;

						if (result.queue.size() < MAX_BLOCKS) {
							result.queue.add(neighbour);
							result.blocks.add(neighbour);
						}

						open.add(neighbour);
					}
				}
			}
		}

		return result;
	}

	/**
	 * Returns a key that groups ore blocks into veins, or {@code null} if the block is not an ore.
	 *
	 * <p>Uses the block id instead of tags, because tags such as {@code c:ores} only exist when the server
	 * has the matching mods installed. Deepslate variants share a key with the normal ore, so a vein crossing
	 * the stone/deepslate border is still mined completely.
	 */
	static String oreFamily(BlockState state) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		String path = id.getPath();

		if (!path.endsWith("_ore") && !path.equals("ancient_debris")) return null;

		if (path.startsWith("deepslate_")) {
			path = path.substring("deepslate_".length());
		}

		return id.getNamespace() + ":" + path;
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
		final String family;
		final Deque<BlockPos> queue = new ArrayDeque<>();
		final Set<BlockPos> blocks = new HashSet<>();
		BlockPos current;

		Job(ClientLevel level, String family) {
			this.level = level;
			this.family = family;
		}
	}
}
