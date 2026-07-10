package dev.kafka.kafkautils.mixin;

import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.module.modules.combat.TotemCounter;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_2663;
import net.minecraft.class_634;
import net.minecraft.class_638;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({class_634.class})
public class ClientPlayNetworkHandlerMixin {
   @Shadow
   private class_638 field_3699;

   @Inject(
      method = {"method_11148"},
      at = {@At("TAIL")}
   )
   private void kafka$onEntityStatus(class_2663 packet, CallbackInfo ci) {
      if (this.field_3699 != null) {
         if (packet.method_11470() == 35) {
            class_1297 entity = packet.method_11469(this.field_3699);
            if (entity instanceof class_1309) {
               class_1309 living = (class_1309)entity;
               TotemCounter counter = (TotemCounter)ModuleManager.get(TotemCounter.class);
               if (counter != null && counter.isEnabled()) {
                  counter.recordPop(living);
               }
            }

         }
      }
   }
}
