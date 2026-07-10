package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.ChatUtil;
import dev.kafka.kafkautils.util.ItemUseHelper;
import net.minecraft.class_1294;
import net.minecraft.class_1802;

public class AutoBadOmen extends Module {
   private final ItemUseHelper use = ItemUseHelper.create();
   private int cooldown = 0;

   public AutoBadOmen() {
      super("Auto-Bad Omen", "Drinks an Ominous Bottle to keep Bad Omen up.", Category.COMBAT);
   }

   public void onTick() {
      if (mc.field_1724 != null) {
         this.use.tick();
         if (this.cooldown > 0) {
            --this.cooldown;
         } else if (!this.use.isBusy()) {
            if (!mc.field_1724.method_6059(class_1294.field_16595)) {
               if (!mc.field_1724.method_6059(class_1294.field_50117)) {
                  if (ItemUseHelper.hotbarHas(class_1802.field_50140) && this.use.startUsing(class_1802.field_50140, 35)) {
                     ChatUtil.info("Пью Зловещий флакон (Bad Omen)");
                     this.cooldown = 60;
                  }

               }
            }
         }
      }
   }

   protected void onDisable() {
      if (mc.field_1690 != null) {
         mc.field_1690.field_1904.method_23481(false);
      }

   }
}
