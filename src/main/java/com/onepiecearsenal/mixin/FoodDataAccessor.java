package com.onepiecearsenal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.food.FoodData;

/** FoodData has no exhaustion getter or setter any more, which Endurance needs to wear exhaustion off. */
@Mixin(FoodData.class)
public interface FoodDataAccessor {
	@Accessor("exhaustionLevel")
	float arsenal$getExhaustionLevel();

	@Accessor("exhaustionLevel")
	void arsenal$setExhaustionLevel(float exhaustionLevel);
}
