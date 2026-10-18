package dev.customcrosshair;

import java.util.List;
import java.util.function.IntFunction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * Custom particles shown when the local player hits an entity.
 */
public final class HitParticles {
	/** A particle "texture" the user can pick; {@code options} receives the chosen ARGB color. */
	public record Type(String name, IntFunction<ParticleOptions> options) {
		static Type simple(String name, SimpleParticleType type) {
			return new Type(name, color -> type);
		}
	}

	public static final List<Type> TYPES = List.of(
			Type.simple("crit", ParticleTypes.CRIT),
			Type.simple("enchanted_hit", ParticleTypes.ENCHANTED_HIT),
			new Type("dust", color -> new DustParticleOptions(color & 0xFFFFFF, 1.0F)),
			Type.simple("end_rod", ParticleTypes.END_ROD),
			Type.simple("firework", ParticleTypes.FIREWORK),
			Type.simple("glow", ParticleTypes.GLOW),
			Type.simple("electric_spark", ParticleTypes.ELECTRIC_SPARK),
			Type.simple("flame", ParticleTypes.FLAME),
			Type.simple("soul_fire_flame", ParticleTypes.SOUL_FIRE_FLAME),
			Type.simple("copper_fire_flame", ParticleTypes.COPPER_FIRE_FLAME),
			Type.simple("heart", ParticleTypes.HEART),
			Type.simple("damage_indicator", ParticleTypes.DAMAGE_INDICATOR),
			Type.simple("angry_villager", ParticleTypes.ANGRY_VILLAGER),
			Type.simple("happy_villager", ParticleTypes.HAPPY_VILLAGER),
			Type.simple("note", ParticleTypes.NOTE),
			Type.simple("snowflake", ParticleTypes.SNOWFLAKE),
			Type.simple("totem_of_undying", ParticleTypes.TOTEM_OF_UNDYING),
			Type.simple("witch", ParticleTypes.WITCH),
			Type.simple("enchant", ParticleTypes.ENCHANT),
			Type.simple("portal", ParticleTypes.PORTAL),
			Type.simple("reverse_portal", ParticleTypes.REVERSE_PORTAL),
			Type.simple("sculk_soul", ParticleTypes.SCULK_SOUL),
			Type.simple("soul", ParticleTypes.SOUL),
			Type.simple("scrape", ParticleTypes.SCRAPE),
			Type.simple("wax_on", ParticleTypes.WAX_ON),
			Type.simple("wax_off", ParticleTypes.WAX_OFF),
			Type.simple("cherry_leaves", ParticleTypes.CHERRY_LEAVES),
			Type.simple("firefly", ParticleTypes.FIREFLY),
			Type.simple("smoke", ParticleTypes.SMOKE),
			Type.simple("cloud", ParticleTypes.CLOUD),
			Type.simple("poof", ParticleTypes.POOF),
			Type.simple("bubble_pop", ParticleTypes.BUBBLE_POP),
			Type.simple("sweep_attack", ParticleTypes.SWEEP_ATTACK),
			new Type("entity_effect", color -> ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color)),
			new Type("flash", color -> ColorParticleOption.create(ParticleTypes.FLASH, color))
	);

	/** Last entity the local player attacked, used to tell our crits apart from other players'. */
	private static Entity lastTarget;
	private static long lastAttackTime;

	private HitParticles() {
	}

	/** Called when the local player attacks an entity. */
	public static void onAttack(Entity target) {
		lastTarget = target;
		lastAttackTime = System.currentTimeMillis();

		CrosshairConfig config = CrosshairConfig.get();

		if (config.particlesEnabled && config.particleTrigger == 1) {
			spawnOn(target);
		}
	}

	/**
	 * Called instead of the vanilla crit / enchanted-hit emitter.
	 *
	 * @return {@code true} to cancel the vanilla particles
	 */
	public static boolean onVanillaHitParticles(Entity entity) {
		boolean ours = entity == lastTarget && System.currentTimeMillis() - lastAttackTime < 1500;
		if (!ours) return false;

		CrosshairConfig config = CrosshairConfig.get();

		if (config.particlesEnabled && config.particleTrigger == 0) {
			spawnOn(entity);
		}

		return config.particlesEnabled && config.hideVanillaParticles;
	}

	public static void spawnOn(Entity entity) {
		spawnBurst(entity.getX(), entity.getY(), entity.getZ(), entity.getBbWidth(), entity.getBbHeight());
	}

	/** Spawns the configured burst inside the given box (feet position + size). */
	public static void spawnBurst(double x, double y, double z, float width, float height) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) return;

		CrosshairConfig config = CrosshairConfig.get();
		Type type = TYPES.get(config.particleType);
		RandomSource random = minecraft.level.getRandom();
		float hueOffset = (System.currentTimeMillis() % 3000L) / 3000.0F;

		for (int i = 0; i < config.particleCount; i++) {
			int color = config.particleRainbow
					? hsv(hueOffset + (float) i / config.particleCount, 0.8F, 1.0F)
					: config.particleColor;

			double px = x + (random.nextDouble() - 0.5) * width;
			double py = y + random.nextDouble() * height;
			double pz = z + (random.nextDouble() - 0.5) * width;

			// Random direction, like the vanilla crit emitter.
			double vx = (random.nextDouble() * 2.0 - 1.0);
			double vy = (random.nextDouble() * 2.0 - 1.0) + 0.2;
			double vz = (random.nextDouble() * 2.0 - 1.0);
			double length = Math.sqrt(vx * vx + vy * vy + vz * vz);
			double speed = 0.4 * config.particleSpeed / Math.max(length, 1.0E-4);

			Particle particle = minecraft.particleEngine.createParticle(type.options().apply(color), px, py, pz, vx * speed, vy * speed, vz * speed);
			if (particle == null) continue;

			particle.scale(config.particleSize);
			particle.setLifetime(Math.max(1, Math.round(particle.getLifetime() * config.particleLifetime)));

			if (config.particleGravity != 0.0F) {
				particle.gravity = config.particleGravity;
			}

			if ((config.particleTint || config.particleRainbow) && particle instanceof SingleQuadParticle quad) {
				quad.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
			}
		}
	}

	/** HSV to opaque ARGB; hue wraps around. */
	public static int hsv(float hue, float saturation, float value) {
		float h = (hue - (float) Math.floor(hue)) * 6.0F;
		int sector = (int) h;
		float f = h - sector;
		float p = value * (1.0F - saturation);
		float q = value * (1.0F - f * saturation);
		float t = value * (1.0F - (1.0F - f) * saturation);
		float r;
		float g;
		float b;

		switch (sector) {
			case 0 -> { r = value; g = t; b = p; }
			case 1 -> { r = q; g = value; b = p; }
			case 2 -> { r = p; g = value; b = t; }
			case 3 -> { r = p; g = q; b = value; }
			case 4 -> { r = t; g = p; b = value; }
			default -> { r = value; g = p; b = q; }
		}

		return 0xFF000000 | Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
	}

	/** Test burst a couple of blocks in front of the player. */
	public static void spawnTest() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) return;

		var look = minecraft.player.getLookAngle();
		double x = minecraft.player.getX() + look.x * 2.5;
		double y = minecraft.player.getEyeY() + look.y * 2.5 - 0.9;
		double z = minecraft.player.getZ() + look.z * 2.5;
		spawnBurst(x, y, z, 0.6F, 1.8F);
	}
}
