package dev.kafka.kafkautils.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_634;
import net.minecraft.class_640;
import net.minecraft.class_742;

/**
 * Reads the current server tab-list and classifies online staff by the
 * Ⓗ / Ⓜ / Ⓐ markers used in their display names, and detects players that are
 * present in the world but missing from the tab-list (a common "vanish"/hidden
 * signal). Everything is derived on demand from the live client state, so there
 * is no cached state to keep in sync.
 */
public final class StaffCounter {
   private static final char HELPER = 'Ⓗ';
   private static final char MOD = 'Ⓜ';
   private static final char ADMIN = 'Ⓐ';

   private StaffCounter() {
   }

   private static String display(class_640 entry) {
      class_2561 name = entry.method_2971();
      if (name != null) {
         return name.getString();
      }
      return entry.method_2966() != null ? entry.method_2966().name() : "";
   }

   /** @return {helpers, mods, admins}. */
   public static int[] counts() {
      int[] out = new int[]{0, 0, 0};
      class_634 handler = class_310.method_1551().method_1562();
      if (handler == null) {
         return out;
      }

      for (class_640 entry : handler.method_2880()) {
         String shown = display(entry);
         if (shown.indexOf(HELPER) >= 0) {
            ++out[0];
         } else if (shown.indexOf(MOD) >= 0) {
            ++out[1];
         } else if (shown.indexOf(ADMIN) >= 0) {
            ++out[2];
         }
      }

      return out;
   }

   /** @return display names of every online player carrying a staff marker. */
   public static List<String> staffNames() {
      List<String> out = new ArrayList<>();
      class_634 handler = class_310.method_1551().method_1562();
      if (handler == null) {
         return out;
      }

      for (class_640 entry : handler.method_2880()) {
         String shown = display(entry);
         if (shown.indexOf(HELPER) >= 0 || shown.indexOf(MOD) >= 0 || shown.indexOf(ADMIN) >= 0) {
            out.add(shown);
         }
      }

      return out;
   }

   /**
    * @return names of players that are present in the world but absent from the
    *         tab-list (a typical indicator of vanished / hidden staff).
    */
   public static List<String> hiddenPlayers() {
      List<String> out = new ArrayList<>();
      class_310 mc = class_310.method_1551();
      class_634 handler = mc.method_1562();
      if (handler == null || mc.field_1687 == null || mc.field_1724 == null) {
         return out;
      }

      Set<UUID> listed = new HashSet<>();
      for (class_640 entry : handler.method_2880()) {
         if (entry.method_2966() != null) {
            listed.add(entry.method_2966().id());
         }
      }

      for (class_742 player : mc.field_1687.method_18456()) {
         if (player == mc.field_1724) {
            continue;
         }
         if (!listed.contains(player.method_5667())) {
            out.add(player.method_5477().getString());
         }
      }

      return out;
   }
}
