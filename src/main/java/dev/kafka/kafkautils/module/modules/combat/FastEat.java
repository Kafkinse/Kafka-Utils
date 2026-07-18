package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.mixin.LivingEntityAccessor;
import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.NumberSetting;

/**
 * Speeds up eating and drinking by shrinking the remaining item-use time each
 * tick. Only short uses (food, potions, milk — 64 ticks or less) are touched,
 * so bows, crossbows, shields, tridents and spyglasses keep working normally.
 */
public class FastEat extends Module {
   private final NumberSetting speed = this.add(new NumberSetting("Speed", 3, 1, 20, 1));

   public FastEat() {
      super("Fast Eat", "Ускоряет поедание еды и питьё зелий.", Category.COMBAT);
   }

   public void onTick() {
      if (mc.field_1724 == null || !mc.field_1724.method_6115()) {
         return;
      }
      // Remaining use ticks: food/drink sit at <=40, while bows/shields/tridents
      // hold ~72000 for the whole use, so this cleanly targets eating/drinking.
      int left = mc.field_1724.method_6014();
      if (left <= 0 || left > 64) {
         return;
      }
      ((LivingEntityAccessor) mc.field_1724).setItemUseTimeLeft(Math.max(0, left - this.speed.get()));
   }
}
