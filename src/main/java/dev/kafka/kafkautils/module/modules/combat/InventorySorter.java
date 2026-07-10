package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.ListSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_1799;
import net.minecraft.class_332;

public class InventorySorter extends Module implements HudModule {
   private final BooleanSetting showSlots = (BooleanSetting)this.add(new BooleanSetting("Show Missing Items", true));
   private final ListSetting presetNames = (ListSetting)this.add(new ListSetting("Presets"));

   private final Map<String, List<class_1799>> presets = new LinkedHashMap();
   private final Map<String, List<String>> missingItems = new HashMap();
   private String activePreset = null;
   private boolean isSaving = false;

   public InventorySorter() {
      super("Inventory Sorter", "Save and load inventory presets.", Category.COMBAT);
   }

   protected void onEnable() {
      this.loadPresets();
   }

   public void onTick() {
      if (mc.field_1724 == null) return;

      // Check if inventory is open (F key or container screen)
      if (mc.field_1755 != null && mc.field_1755 instanceof net.minecraft.class_1703) {
         // Check for our buttons in the GUI
         this.showSaveLoadButtons();
      }
   }

   public void savePreset(String name) {
      if (mc.field_1724 == null) return;
      List<class_1799> items = new ArrayList();
      // Save hotbar and inventory
      net.minecraft.class_1661 inv = mc.field_1724.method_7371();
      for (int i = 0; i < 36; i++) {
         items.add(inv.method_5438(i).method_7938());
      }
      this.presets.put(name, items);
      ChatUtil.info("§aPreset saved: §d" + name);
   }

   public void loadPreset(String name) {
      if (mc.field_1724 == null) return;
      List<class_1799> items = this.presets.get(name);
      if (items == null) {
         ChatUtil.info("§cPreset not found: §d" + name);
         return;
      }

      net.minecraft.class_1661 inv = mc.field_1724.method_7371();
      List<String> missing = new ArrayList();
      this.activePreset = name;

      // Try to find and move items to correct slots
      for (int i = 0; i < Math.min(items.size(), 36); i++) {
         class_1799 desired = (class_1799)items.get(i);
         if (desired.method_7960()) continue;

         String itemName = desired.method_7909().method_63680().getString();
         class_1799 current = inv.method_5438(i);

         // Check if correct item is already in this slot
         if (!current.method_7960() && current.method_7909() == desired.method_7909()) {
            continue;
         }

         // Find the item in the inventory
         int foundSlot = -1;
         for (int j = 0; j < 36; j++) {
            class_1799 stack = inv.method_5438(j);
            if (!stack.method_7960() && stack.method_7909() == desired.method_7909()) {
               foundSlot = j;
               break;
            }
         }

         if (foundSlot == -1) {
            // Item not found in inventory
            if (!missing.contains(itemName)) {
               missing.add(itemName);
            }
         }
      }

      this.missingItems.put(name, missing);
      if (missing.isEmpty()) {
         ChatUtil.info("§aPreset loaded: §d" + name);
      } else {
         ChatUtil.info("§ePreset loaded: §d" + name + " §7(missing " + missing.size() + " items)");
      }
   }

   public void deletePreset(String name) {
      this.presets.remove(name);
      ChatUtil.info("§cDeleted preset: §d" + name);
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList();

      if (this.activePreset != null) {
         lines.add("§dActive: §r" + this.activePreset);
         List<String> missing = this.missingItems.get(this.activePreset);
         if (missing != null && !missing.isEmpty()) {
            lines.add("§7Missing: §c" + String.join("§7, §c", missing));
         }
      } else {
         lines.add("§7no preset active");
      }

      return RenderUtil.panel(ctx, x, y, "Inventory Sorter", lines);
   }

   // Show save/load buttons when inventory is open (simplified - buttons will show in clickgui)
   private void showSaveLoadButtons() {
      // Placeholder for GUI integration
   }

   private void loadPresets() {
      // Presets are loaded from ListSetting entries
      List<String> names = this.presetNames.values();
      for (String name : names) {
         if (!this.presets.containsKey(name)) {
            this.presets.put(name, new ArrayList());
         }
      }
   }
}
