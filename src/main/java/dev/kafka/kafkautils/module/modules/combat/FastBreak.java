package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.mixin.ClientPlayerInteractionManagerAccessor;
import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;

/**
 * Removes the short cooldown the client normally waits between breaking blocks,
 * so mining is limited only by your real break speed. Effective where the
 * server lets the client drive block breaking.
 */
public class FastBreak extends Module {
   public FastBreak() {
      super("Fast Break", "Убирает задержку между ломанием блоков.", Category.COMBAT);
   }

   public void onTick() {
      if (mc.field_1761 != null) {
         ((ClientPlayerInteractionManagerAccessor) mc.field_1761).setBlockBreakingCooldown(0);
      }
   }
}
