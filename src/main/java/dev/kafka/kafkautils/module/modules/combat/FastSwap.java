package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_7923;

/**
 * A selection wheel (opened with the same key as the potion wheel — see the
 * keybind logic in the client) that, instead of using an item, simply moves the
 * picked item into your <b>active hand slot</b>. It never consumes anything, so
 * things like golden apples are only placed in hand, not eaten.
 *
 * <p>The wheel opens on the potion-wheel key only when Auto Pot is disabled; with
 * Auto Pot enabled that key throws potions instead.
 */
public class FastSwap extends Module {
   public FastSwap() {
      super("Fast Swap", "Кольцо для быстрого перемещения предмета в активную руку (клавиша колеса зелий, когда Auto Pot выключен).", Category.COMBAT);
   }

   /** One distinct item type per slice, with the total count across the inventory. */
   public List<Opt> listItems() {
      List<Opt> out = new ArrayList<>();
      if (mc.field_1724 == null) {
         return out;
      }
      Map<String, Opt> byType = new LinkedHashMap<>();
      for (int i = 0; i < 36; ++i) {
         class_1799 s = mc.field_1724.method_31548().method_5438(i);
         if (s.method_7960()) {
            continue;
         }
         String key = class_7923.field_41178.method_10221(s.method_7909()).method_12832();
         Opt o = byType.get(key);
         if (o == null) {
            byType.put(key, new Opt(i, s, s.method_7964().getString(), s.method_7947()));
         } else {
            o.count += s.method_7947();
         }
      }
      out.addAll(byType.values());
      return out;
   }

   /** Swaps the picked inventory item into the currently selected hotbar slot. */
   public void swapToHand(int invIndex) {
      if (mc.field_1724 == null || mc.field_1761 == null || invIndex < 0 || invIndex > 35) {
         return;
      }
      int hotbar = mc.field_1724.method_31548().method_67532(); // selected hotbar slot 0-8
      if (invIndex == hotbar) {
         return; // already in hand
      }
      int screenSlot = invIndex < 9 ? invIndex + 36 : invIndex;
      int sync = mc.field_1724.field_7512.field_7763;
      // SWAP with button = hotbar index puts the clicked slot into the held slot.
      mc.field_1761.method_2906(sync, screenSlot, hotbar, class_1713.field_7791, mc.field_1724);
   }

   /** A wheel option: an example slot/stack for the icon, a display name and total count. */
   public static final class Opt {
      public final int index;
      public final class_1799 stack;
      public final String name;
      public int count;

      public Opt(int index, class_1799 stack, String name, int count) {
         this.index = index;
         this.stack = stack;
         this.name = name;
         this.count = count;
      }
   }
}
