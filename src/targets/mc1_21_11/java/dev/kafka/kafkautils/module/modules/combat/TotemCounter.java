package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_1309;
import net.minecraft.class_332;

public class TotemCounter extends Module implements HudModule {
   private final Map<String, Integer> counts = new LinkedHashMap<>();
   private int clearTimer = 0;

   public TotemCounter() {
      super("Totem Counter", "Counts totem pops (announcements are in PvP Logger).", Category.COMBAT);
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

   /** Called from the entity-status mixin when a totem pops on a living entity. */
   public void recordPop(class_1309 entity) {
      String name = entity.method_5477().getString();
      this.counts.merge(name, 1, Integer::sum);
      PvPLogger pvp = ModuleManager.get(PvPLogger.class);
      if (pvp != null) {
         pvp.logTotem(name);
      }
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList<>();
      if (this.counts.isEmpty()) {
         lines.add("§7нет попов");
      } else {
         for (Map.Entry<String, Integer> e : this.counts.entrySet()) {
            lines.add("§d" + e.getKey() + "§7: §r" + e.getValue());
         }
      }
      return RenderUtil.panel(ctx, x, y, "Totem Counter", lines);
   }
}
