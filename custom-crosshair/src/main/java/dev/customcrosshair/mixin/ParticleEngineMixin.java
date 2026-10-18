package dev.customcrosshair.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;

import dev.customcrosshair.HitParticles;

/**
 * Vanilla shows crit / enchanted-hit particles through a tracking emitter on the hit entity.
 * We replace those with our own particles when the hit came from the local player.
 */
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
	@Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V",
			at = @At("HEAD"), cancellable = true)
	private void customcrosshair$replaceHitParticles(Entity entity, ParticleOptions options, CallbackInfo ci) {
		customcrosshair$handle(entity, options, ci);
	}

	@Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V",
			at = @At("HEAD"), cancellable = true)
	private void customcrosshair$replaceHitParticlesTimed(Entity entity, ParticleOptions options, int lifetime, CallbackInfo ci) {
		customcrosshair$handle(entity, options, ci);
	}

	private static void customcrosshair$handle(Entity entity, ParticleOptions options, CallbackInfo ci) {
		if (options.getType() != ParticleTypes.CRIT && options.getType() != ParticleTypes.ENCHANTED_HIT) return;

		if (HitParticles.onVanillaHitParticles(entity)) {
			ci.cancel();
		}
	}
}
