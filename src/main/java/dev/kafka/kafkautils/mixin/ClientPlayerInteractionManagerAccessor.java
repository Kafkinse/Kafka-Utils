package dev.kafka.kafkautils.mixin;

import net.minecraft.class_636;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets Auto Mine zero the block-breaking cooldown for Fast Break. */
@Mixin(class_636.class)
public interface ClientPlayerInteractionManagerAccessor {
   @Accessor("field_3716")
   void setBlockBreakingCooldown(int cooldown);
}
