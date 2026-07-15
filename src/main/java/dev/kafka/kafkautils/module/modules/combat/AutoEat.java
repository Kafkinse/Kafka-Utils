package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import java.util.Locale;
import net.minecraft.class_1799;
import net.minecraft.class_7923;

/**
 * Eats food from the hotbar when hunger drops below a threshold, then switches
 * back to whatever you were holding. Auto Mine pauses while this runs (via
 * {@link #isBusy}) so the two don't fight over the held slot.
 */
public class AutoEat extends Module {
   private final NumberSetting threshold = this.add(new NumberSetting("Eat Below", 10, 1, 19, 1));
   private final StringSetting foods = this.add(new StringSetting("Foods",
      "carrot,apple,bread,beef,porkchop,chicken,mutton,rabbit,cooked,cod,salmon,potato,beetroot,berries,melon,stew,soup,pie,honey_bottle,dried_kelp"));

   private boolean eating;
   private int savedSlot = -1;

   public AutoEat() {
      super("Auto Eat", "Eats food from the hotbar when hunger is low.", Category.FARMING);
   }

   protected void onDisable() {
      this.stop();
   }

   public boolean isBusy() {
      return this.eating;
   }

   public void onTick() {
      if (mc.field_1724 == null || mc.field_1690 == null || mc.field_1755 != null) {
         if (this.eating) {
            this.stop();
         }
         return;
      }

      int hunger = mc.field_1724.method_7344().method_7586();

      if (this.eating) {
         // Keep the use key held so the food is consumed.
         mc.field_1690.field_1904.method_23481(true);
         if (hunger > this.threshold.get() || !isFood(mc.field_1724.method_31548().method_5438(mc.field_1724.method_31548().method_67532()))) {
            this.stop();
         }
         return;
      }

      if (hunger <= this.threshold.get()) {
         int slot = this.findFood();
         if (slot >= 0) {
            this.savedSlot = mc.field_1724.method_31548().method_67532();
            mc.field_1724.method_31548().method_61496(slot);
            this.eating = true;
         }
      }
   }

   private void stop() {
      if (mc.field_1690 != null) {
         mc.field_1690.field_1904.method_23481(false);
      }
      if (this.eating && this.savedSlot >= 0 && mc.field_1724 != null) {
         mc.field_1724.method_31548().method_61496(this.savedSlot);
      }
      this.savedSlot = -1;
      this.eating = false;
   }

   private int findFood() {
      for (int i = 0; i < 9; ++i) {
         if (isFood(mc.field_1724.method_31548().method_5438(i))) {
            return i;
         }
      }
      return -1;
   }

   private boolean isFood(class_1799 stack) {
      if (stack.method_7960()) {
         return false;
      }
      String id = class_7923.field_41178.method_10221(stack.method_7909()).method_12832();
      for (String t : this.foods.get().split("[,\\s]+")) {
         if (!t.isBlank() && id.contains(t.trim().toLowerCase(Locale.ROOT))) {
            return true;
         }
      }
      return false;
   }
}
