package dev.kafka.kafkautils.util;

import java.util.Locale;
import net.minecraft.class_1268;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import net.minecraft.class_7923;

/**
 * Shared packet-item helpers used by Pot Heal, Auto Pot and Quick Pearl. Uses an
 * off-hand swap so an item can be thrown/used from anywhere in the inventory
 * without disturbing the main hand (your weapon).
 */
public final class PotionActions {
   private static final class_1268 OFF_HAND = class_1268.values()[1];

   private PotionActions() {
   }

   /** True for splash/lingering potions (the only kind usable instantly from inventory). */
   public static boolean isThrowable(class_1799 s) {
      if (s == null || s.method_7960()) {
         return false;
      }
      String id = class_7923.field_41178.method_10221(s.method_7909()).method_12832();
      return id.equals("splash_potion") || id.equals("lingering_potion");
   }

   /** True if the stack's display name contains any comma-separated keyword. */
   public static boolean matches(class_1799 s, String csv) {
      String name = s.method_7964().getString().toLowerCase(Locale.ROOT);
      for (String k : csv.split(",")) {
         String kk = k.trim().toLowerCase(Locale.ROOT);
         if (!kk.isEmpty() && name.contains(kk)) {
            return true;
         }
      }
      return false;
   }

   /** Inventory index of the first throwable potion matching the keywords, or -1. */
   public static int findThrowable(String csv) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 == null) {
         return -1;
      }
      for (int i = 0; i < 36; ++i) {
         class_1799 s = mc.field_1724.method_31548().method_5438(i);
         if (isThrowable(s) && matches(s, csv)) {
            return i;
         }
      }
      return -1;
   }

   /** Packet-throws/uses an item from any inventory slot via a temporary off-hand swap. */
   public static void useFromInventory(int invIndex, boolean lookDown) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 == null || mc.field_1761 == null || invIndex < 0 || invIndex > 35) {
         return;
      }
      int screenSlot = invIndex < 9 ? invIndex + 36 : invIndex; // inventory index -> screen slot
      int sync = mc.field_1724.field_7512.field_7763;
      float savedPitch = mc.field_1724.method_36455();
      if (lookDown) {
         mc.field_1724.method_36457(90.0F);
      }
      mc.field_1761.method_2906(sync, screenSlot, 40, class_1713.field_7791, mc.field_1724); // -> off-hand
      mc.field_1761.method_2919(mc.field_1724, OFF_HAND);                                    // throw/use
      mc.field_1761.method_2906(sync, screenSlot, 40, class_1713.field_7791, mc.field_1724); // <- back
      if (lookDown) {
         mc.field_1724.method_36457(savedPitch);
      }
   }
}
