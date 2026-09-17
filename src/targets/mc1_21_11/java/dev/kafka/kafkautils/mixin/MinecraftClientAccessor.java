package dev.kafka.kafkautils.mixin;

import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets Auto Mine zero the item-use cooldown for Fast Place. */
@Mixin(class_310.class)
public interface MinecraftClientAccessor {
   @Accessor("field_1752")
   void setItemUseCooldown(int cooldown);
}
