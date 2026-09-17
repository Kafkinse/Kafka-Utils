package dev.kafka.kafkautils.util;

import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_310;

public final class ItemUseHelper {
   private int holdTicks = 0;

   public boolean startUsing(class_1792 item, int ticks) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 == null) {
         return false;
      } else {
         int slot = findHotbar(item);
         if (slot < 0) {
            return false;
         } else {
            mc.field_1724.method_31548().method_61496(slot);
            this.holdTicks = ticks;
            return true;
         }
      }
   }

   public boolean isBusy() {
      return this.holdTicks > 0;
   }

   public void tick() {
      class_310 mc = class_310.method_1551();
      if (this.holdTicks > 0) {
         mc.field_1690.field_1904.method_23481(true);
         --this.holdTicks;
         if (this.holdTicks == 0) {
            mc.field_1690.field_1904.method_23481(false);
         }
      }

   }

   public static int findHotbar(class_1792 item) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 == null) {
         return -1;
      } else {
         for(int i = 0; i < 9; ++i) {
            class_1799 stack = mc.field_1724.method_31548().method_5438(i);
            if (stack.method_7909() == item) {
               return i;
            }
         }

         return -1;
      }
   }

   public static boolean hotbarHas(class_1792 item) {
      return findHotbar(item) >= 0;
   }

   private ItemUseHelper() {
   }

   public static ItemUseHelper create() {
      return new ItemUseHelper();
   }
}
