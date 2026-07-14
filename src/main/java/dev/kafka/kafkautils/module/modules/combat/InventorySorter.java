package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.util.SortSession;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1661;
import net.minecraft.class_1703;
import net.minecraft.class_1735;

/**
 * Sorts your inventory on the "Sort" keybind (Options → Controls). It orders your
 * main inventory (and, optionally, the hotbar) by item type and stack size and
 * consolidates partial stacks. Works whether the inventory GUI is open or closed;
 * when a container is open, Storage AutoSort handles the container instead.
 */
public class InventorySorter extends Module {
   private final BooleanSetting includeHotbar = this.add(new BooleanSetting("Include Hotbar", false));
   private final NumberSetting delay = this.add(new NumberSetting("Delay", 2, 1, 20, 1));

   private SortSession session;
   private int tick;

   public InventorySorter() {
      super("Inventory Sort", "Sorts your inventory on the Sort key (Options → Controls).", Category.FARMING);
   }

   protected void onEnable() {
      this.session = null;
   }

   public void onTick() {
      if (mc.field_1724 == null || mc.field_1761 == null || this.session == null) {
         return;
      }
      if (this.session.isDone()) {
         this.session = null;
         return;
      }
      if (++this.tick >= this.delay.get()) {
         this.tick = 0;
         this.session.step(mc.field_1761, mc.field_1724);
      }
   }

   /** Starts a sort of your inventory. Called from the Sort keybind. */
   public void sortNow() {
      if (mc.field_1724 == null) {
         return;
      }
      int[] slots = this.playerSlots();
      if (slots.length > 1) {
         this.session = new SortSession(mc.field_1724.field_7512, slots);
         this.tick = 0;
      }
   }

   private int[] playerSlots() {
      class_1703 handler = mc.field_1724.field_7512;
      class_1661 inv = mc.field_1724.method_31548();
      int minIndex = this.includeHotbar.get() ? 0 : 9;
      List<Integer> ids = new ArrayList<>();
      for (int i = 0; i < handler.field_7761.size(); ++i) {
         class_1735 slot = handler.field_7761.get(i);
         int idx = slot.method_34266();
         if (slot.field_7871 == inv && idx >= minIndex && idx <= 35) {
            ids.add(i);
         }
      }
      int[] out = new int[ids.size()];
      for (int i = 0; i < out.length; ++i) {
         out[i] = ids.get(i);
      }
      return out;
   }
}
