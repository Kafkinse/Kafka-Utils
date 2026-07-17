package dev.kafka.kafkautils.mixin;

import dev.kafka.kafkautils.module.modules.chat.ModRadar;
import net.minecraft.class_2561;
import net.minecraft.class_355;
import net.minecraft.class_640;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prepends a §d✦ badge in the tab list for players detected running Kafka-Utils. */
@Mixin(class_355.class)
public class PlayerListHudMixin {
   @Inject(
      method = {"method_1918"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void kafka$badge(class_640 entry, CallbackInfoReturnable<class_2561> cir) {
      if (entry.method_2966() != null && ModRadar.shouldBadge(entry.method_2966().name())) {
         cir.setReturnValue(class_2561.method_43470("§d✦ ").method_10852(cir.getReturnValue()));
      }
   }
}
