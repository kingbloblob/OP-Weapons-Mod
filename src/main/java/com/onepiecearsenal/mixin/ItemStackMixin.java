package com.onepiecearsenal.mixin;

import java.util.function.BiConsumer;

import org.apache.commons.lang3.function.TriConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.state.BlockState;

import com.onepiecearsenal.haki.HakiImbue;
import com.onepiecearsenal.salvage.Breakage;

/**
 * Broken Arsenal gear (see Breakage), and Haki-coated weapons that take no wear (see HakiImbue). Every way an item takes wear ends in setDamageValue, and vanilla only destroys
 * the item if the wear reaches its durability, so stopping one short there keeps forged gear from vanishing.
 * forEachModifier is where worn and held items hand their attributes to their owner (and to the tooltip), so broken
 * gear hands over none.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@ModifyVariable(method = "setDamageValue(I)V", at = @At("HEAD"), argsOnly = true)
	private int arsenal$keepBrokenGear(int damage) {
		ItemStack stack = (ItemStack) (Object) this;
		return HakiImbue.keepUnworn(stack, Breakage.keepBroken(stack, damage));
	}

	/** Broken tools dig like a bare hand and get no drops a tool is needed for. */
	@Inject(method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F", at = @At("HEAD"), cancellable = true)
	private void arsenal$brokenToolsDigLikeAHand(BlockState state, CallbackInfoReturnable<Float> cir) {
		if (Breakage.isBroken((ItemStack) (Object) this)) {
			cir.setReturnValue(1.0f);
		}
	}

	@Inject(method = "isCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z", at = @At("HEAD"), cancellable = true)
	private void arsenal$brokenToolsGetNoDrops(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (Breakage.isBroken((ItemStack) (Object) this)) {
			cir.setReturnValue(false);
		}
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
