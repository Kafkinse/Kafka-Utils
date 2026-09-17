package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.ChatUtil;
import dev.kafka.kafkautils.util.PotionActions;
import net.minecraft.class_1799;
import net.minecraft.class_7923;

/**
 * Throws an ender pearl from anywhere in the inventory on a keybind, without
 * disturbing the item in your main hand (same off-hand packet trick as Auto Pot).
 *
 * <p>NOTE: packet automation not exercisable in the build environment; mappings
 * verified for 1.21.11.
 */
public class QuickPearl extends Module {
   public QuickPearl() {
      super("Quick Pearl", "Бросок эндер-жемчуга из инвентаря по клавише (не убирая меч).", Category.COMBAT);
   }

   /** Finds an ender pearl and throws it via a temporary off-hand swap. */
   public void throwPearl() {
      if (mc.field_1724 == null || mc.field_1761 == null) {
         return;
      }
      int idx = this.findPearl();
      if (idx < 0) {
         ChatUtil.info("§d[Quick Pearl] §7нет эндер-жемчуга в инвентаре.");
         return;
      }
      PotionActions.useFromInventory(idx, false);
   }

   private int findPearl() {
      for (int i = 0; i < 36; ++i) {
         class_1799 s = mc.field_1724.method_31548().method_5438(i);
         if (!s.method_7960() && class_7923.field_41178.method_10221(s.method_7909()).method_12832().equals("ender_pearl")) {
            return i;
         }
      }
      return -1;
   }
}
