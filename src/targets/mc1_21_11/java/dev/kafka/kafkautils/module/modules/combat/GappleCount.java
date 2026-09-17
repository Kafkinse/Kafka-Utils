package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_332;
import net.minecraft.class_742;

public class GappleCount extends Module implements HudModule {
   private final Map<UUID, Boolean> wasUsing = new HashMap();
   private final Map<UUID, Integer> lastTimeLeft = new HashMap();
   private final Map<UUID, class_1792> lastItem = new HashMap();
   private final Map<String, Integer> counts = new LinkedHashMap();
   private final List<String> eating = new ArrayList();

   public GappleCount() {
      super("Gapple Count", "Counts golden apples players eat.", Category.COMBAT);
   }

   protected void onEnable() {
      this.wasUsing.clear();
      this.lastTimeLeft.clear();
      this.lastItem.clear();
      this.counts.clear();
   }

   public void onTick() {
      if (mc.field_1687 != null && mc.field_1724 != null) {
         this.eating.clear();

         for(class_742 p : mc.field_1687.method_18456()) {
            if (p != mc.field_1724) {
               UUID id = p.method_5667();
               boolean using = p.method_6115();
               if (using) {
                  class_1792 it = p.method_6030().method_7909();
                  this.lastItem.put(id, it);
                  this.lastTimeLeft.put(id, p.method_6014());
                  if (isGapple(it)) {
                     this.eating.add(p.method_5477().getString());
                  }
               }

               boolean prev = (Boolean)this.wasUsing.getOrDefault(id, false);
               if (prev && !using) {
                  int tl = (Integer)this.lastTimeLeft.getOrDefault(id, 99);
                  class_1792 it = (class_1792)this.lastItem.get(id);
                  if (tl <= 2 && it != null && isGapple(it)) {
                     this.counts.merge(p.method_5477().getString(), 1, Integer::sum);
                  }
               }

               this.wasUsing.put(id, using);
            }
         }

      }
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList();

      for(String n : this.eating) {
         lines.add("§a" + n + " ест яблоко! (Absorption)");
      }

      if (this.counts.isEmpty()) {
         lines.add("§7нет данных");
      } else {
         for(Map.Entry<String, Integer> e : this.counts.entrySet()) {
            String var10001 = (String)e.getKey();
            lines.add("§d" + var10001 + "§r: " + String.valueOf(e.getValue()));
         }
      }

      return RenderUtil.panel(ctx, x, y, "Gapples", lines);
   }

   private static boolean isGapple(class_1792 i) {
      return i == class_1802.field_8463 || i == class_1802.field_8367;
   }
}
