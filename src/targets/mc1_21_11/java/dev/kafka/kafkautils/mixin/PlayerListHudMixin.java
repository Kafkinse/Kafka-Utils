package dev.kafka.kafkautils.mixin;

import dev.kafka.kafkautils.module.modules.chat.ModRadar;
import dev.kafka.kafkautils.util.NicknameColorFix;
import net.minecraft.class_2561;
import net.minecraft.class_355;
import net.minecraft.class_640;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prepends a §d✦ badge in the tab list for players detected running
 * Kafka-Utils, and rewrites black nickname color to white so it stays
 * readable against the tab list's dark background.
 */
@Mixin(class_355.class)
public class PlayerListHudMixin {
   @Inject(
      method = {"method_1918"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void kafka$badge(class_640 entry, CallbackInfoReturnable<class_2561> cir) {
      class_2561 name = NicknameColorFix.whitenBlack(cir.getReturnValue());
      if (entry.method_2966() != null && ModRadar.shouldBadge(entry.method_2966().name())) {
         name = class_2561.method_43470("§d✦ ").method_10852(name);
      }
      cir.setReturnValue(name);
   }
}
