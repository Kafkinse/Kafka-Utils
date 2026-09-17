package dev.kafka.kafkautils.mixin;

import net.minecraft.class_1309;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets Fast Eat shorten the remaining item-use time of the local player. */
@Mixin(class_1309.class)
public interface LivingEntityAccessor {
   @Accessor("field_6222")
   void setItemUseTimeLeft(int ticks);
}
