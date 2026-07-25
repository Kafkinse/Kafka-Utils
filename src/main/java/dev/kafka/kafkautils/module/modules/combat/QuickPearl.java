package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.ChatUtil;
import net.minecraft.class_1268;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_7923;

/**
 * Throws an ender pearl from anywhere in the inventory on a keybind, without
 * disturbing the item in your main hand. The pearl is swapped into the off-hand,
 * thrown, and swapped back in the same tick (same packet trick as Auto Pot).
 *
 * <p>NOTE: packet automation not exercisable in the build environment; mappings
 * verified for 1.21.11.
 */
public class QuickPearl extends Module {
   private static final class_1268 OFF_HAND = class_1268.values()[1];

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
      int screenSlot = idx < 9 ? idx + 36 : idx;
      int sync = mc.field_1724.field_7512.field_7763;
      mc.field_1761.method_2906(sync, screenSlot, 40, class_1713.field_7791, mc.field_1724); // -> off-hand
      mc.field_1761.method_2919(mc.field_1724, OFF_HAND);                                    // throw
      mc.field_1761.method_2906(sync, screenSlot, 40, class_1713.field_7791, mc.field_1724); // <- back
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
