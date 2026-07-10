package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_1309;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;

public class TotemCounter extends Module implements HudModule {
   private final Map<String, Integer> counts = new LinkedHashMap();
   private boolean chatOutput = true;
   private int clearTimer = 0;

   public TotemCounter() {
      super("Totem Counter", "Counts and announces totem pops.", Category.COMBAT);
   }

   protected void onEnable() {
      this.counts.clear();
      this.clearTimer = 0;
   }

   public void onTick() {
      if (++this.clearTimer >= 200) {
         this.clearTimer = 0;
         this.counts.clear();
      }
   }

   public void recordPop(class_1309 entity) {
      String name = entity.method_5477().getString();
      int total = (Integer)this.counts.merge(name, 1, Integer::sum);
      if (this.chatOutput) {
         ChatUtil.raw("§5" + name + "§r взорвал тотем! §dОсталось: " + total);
      }
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList();
      if (this.counts.isEmpty()) {
         lines.add("§7нет попов");
      } else {
         // Center-align: format with padding for clean display
         int maxW = 0;
         class_327 font = class_310.method_1551().field_1772;
         for(Map.Entry<String, Integer> e : this.counts.entrySet()) {
            String line = "§d" + (String)e.getKey() + "§r: " + String.valueOf(e.getValue());
            int lw = font.method_1727(line.replace("§", "").replaceAll(".", "")); // will redo
            maxW = Math.max(maxW, font.method_1727("§d" + (String)e.getKey() + "§r: " + String.valueOf(e.getValue())));
         }
         for(Map.Entry<String, Integer> e : this.counts.entrySet()) {
            String name = (String)e.getKey();
            String num = String.valueOf(e.getValue());
            // Right-align numbers for clean look
            int pad = maxW - font.method_1727(name) - font.method_1727(num) - font.method_1727(": ");
            String fmt = "§d" + name + "§r:" + (pad > 0 ? "§7" + " ".repeat(pad / 4) : " ") + num;
            lines.add(fmt);
         }
      }
      return RenderUtil.panel(ctx, x, y, "Totem Counter", lines);
   }
}
