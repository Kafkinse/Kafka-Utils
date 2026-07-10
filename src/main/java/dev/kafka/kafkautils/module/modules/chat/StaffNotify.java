package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.ChatUtil;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.class_2561;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_640;

public class StaffNotify extends Module {
   private static final char[] STAFF_MARKERS = new char[]{'Ⓗ', 'Ⓜ', 'Ⓐ'};
   private final Set<String> seen = new HashSet();

   public StaffNotify() {
      super("Staff Notify", "Notifies when staff (Ⓗ/Ⓜ/Ⓐ) are online.", Category.CHAT);
   }

   protected void onEnable() {
      this.seen.clear();
   }

   protected void onDisable() {
      this.seen.clear();
   }

   public void onTick() {
      if (mc.method_1562() != null && mc.field_1724 != null) {
         Set<String> current = new HashSet();

         for(class_640 entry : mc.method_1562().method_2880()) {
            if (entry.method_2966() != null) {
               String name = entry.method_2966().name();
               current.add(name);
               if (!this.seen.contains(name)) {
                  this.seen.add(name);
                  class_2561 displayName = entry.method_2971();
                  String shown = displayName != null ? displayName.getString() : name;
                  if (this.hasStaffMarker(shown)) {
                     ChatUtil.raw("§5§l[STAFF] §r" + shown + "§d на сервере!");
                     mc.field_1724.method_5783((class_3414)class_3417.field_14622.comp_349(), 1.0F, 1.5F);
                  }
               }
            }
         }

         this.seen.retainAll(current);
      }
   }

   private boolean hasStaffMarker(String text) {
      for(char marker : STAFF_MARKERS) {
         if (text.indexOf(marker) >= 0) {
            return true;
         }
      }

      return false;
   }
}
