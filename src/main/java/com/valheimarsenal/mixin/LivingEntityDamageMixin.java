package com.valheimarsenal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import com.valheimarsenal.combat.DamageRules;

/**
 * Applies Valheim damage rules (backstab, spirit, skeleton weaknesses, combos, ripostes)
 * to the incoming damage amount at the very start of LivingEntity#hurtServer.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float valheimArsenal$applyDamageRules(float amount,
			@Local(argsOnly = true) ServerLevel level,
			@Local(argsOnly = true) DamageSource source) {
		return DamageRules.modifyIncoming((LivingEntity) (Object) this, level, source, amount);
	}
}
