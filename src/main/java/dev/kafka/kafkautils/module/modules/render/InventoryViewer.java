package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.class_332;
import net.minecraft.class_742;

public class InventoryViewer extends Module implements HudModule {
   private UUID trackedId = null;
   private String trackedName = "";
   private long trackTime = 0L;

   public InventoryViewer() {
      super("Inventory Viewer", "Constantly shows the hotbar of your enemy (not just on aim).", Category.RENDER);
   }

   protected void onEnable() {
      this.trackedId = null;
   }

   public void onTick() {
      if (mc.field_1687 != null && mc.field_1724 != null) {
         // Get current target from crosshair
         if (mc.field_1765 != null && mc.field_1765.method_17783() == net.minecraft.class_239.class_240.field_1331) {
            net.minecraft.class_239 var2 = mc.field_1765;
            if (var2 instanceof net.minecraft.class_3966) {
               net.minecraft.class_3966 ehr = (net.minecraft.class_3966)var2;
               net.minecraft.class_1297 e = ehr.method_17782();
               if (e instanceof net.minecraft.class_1657) {
                  this.trackedId = e.method_5667();
                  this.trackedName = e.method_5477().getString();
                  this.trackTime = System.currentTimeMillis();
               }
            }
         }

         // Keep tracking for 15 seconds after losing sight
         if (this.trackedId != null && System.currentTimeMillis() - this.trackTime > 15000L) {
            this.trackedId = null;
         }
      }
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      if (this.trackedId == null || mc.field_1687 == null) {
         return new int[]{0, 0};
      }

      // Find the tracked player
      class_742 target = null;
      for (class_742 p : mc.field_1687.method_18456()) {
         if (p.method_5667().equals(this.trackedId)) {
            target = p;
            break;
         }
      }

      if (target == null) {
         // Lost target, show stale indicator
         List<String> lines = new ArrayList();
         lines.add("§7" + this.trackedName + " §8(disconnected)");
         return RenderUtil.panel(ctx, x, y, "Inventory Viewer", lines);
      }

      List<String> lines = new ArrayList();
      net.minecraft.class_1799 main = target.method_6047();
      net.minecraft.class_1799 off = target.method_6079();

      // Hotbar
      StringBuilder hotbar = new StringBuilder("§7[");
      for (int i = 0; i < 9; i++) {
         net.minecraft.class_1799 stack = target.method_7371().method_5438(i);
         if (stack.method_7960()) {
            hotbar.append("§8_");
         } else {
            String itemName = stack.method_7909().method_63680().getString();
            if (itemName.length() > 4) itemName = itemName.substring(0, 4) + ".";
            boolean isSelected = i == target.method_7371().field_7545;
            hotbar.append(isSelected ? "§a" : "§f").append(itemName);
         }
         if (i < 8) hotbar.append("§7|");
      }
      hotbar.append("§7]");
      lines.add(hotbar.toString());

      if (!main.method_7960()) {
         lines.add("§dMain: §r" + main.method_7964().getString());
      }
      if (!off.method_7960()) {
         lines.add("§dOff: §r" + off.method_7964().getString());
      }

      return RenderUtil.panel(ctx, x, y, "§d" + this.trackedName, lines);
   }
}
