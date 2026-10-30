package com.onepiecearsenal.mixin;

import java.util.function.BiConsumer;

import org.apache.commons.lang3.function.TriConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import com.onepiecearsenal.salvage.Breakage;

/**
 * Broken Arsenal gear (see Breakage). Every way an item takes wear ends in setDamageValue, and vanilla only destroys
 * the item if the wear reaches its durability, so stopping one short there keeps forged gear from vanishing.
 * forEachModifier is where worn and held items hand their attributes to their owner (and to the tooltip), so broken
 * gear hands over none.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@ModifyVariable(method = "setDamageValue(I)V", at = @At("HEAD"), argsOnly = true)
	private int arsenal$keepBrokenGear(int damage) {
		return Breakage.keepBroken((ItemStack) (Object) this, damage);
	}

	@Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
			at = @At("HEAD"), cancellable = true)
	private void arsenal$brokenGearGivesNoAttributes(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> action, CallbackInfo ci) {
		if (Breakage.isBroken((ItemStack) (Object) this)) {
			ci.cancel();
		}
	}

	@Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V",
			at = @At("HEAD"), cancellable = true)
	private void arsenal$brokenGearShowsNoAttributes(EquipmentSlotGroup group, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> action, CallbackInfo ci) {
		if (Breakage.isBroken((ItemStack) (Object) this)) {
			ci.cancel();
		}
	}
}
