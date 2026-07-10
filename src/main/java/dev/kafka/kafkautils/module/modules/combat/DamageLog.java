package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_332;
import net.minecraft.class_742;

public class DamageLog extends Module implements HudModule {
   private float lastHealth = -1.0F;
   private final Deque<Entry> log = new ArrayDeque();

   public DamageLog() {
      super("Damage Log", "Logs incoming damage (best-effort source).", Category.COMBAT);
   }

   protected void onEnable() {
      this.lastHealth = -1.0F;
      this.log.clear();
   }

   public void onTick() {
      if (mc.field_1724 != null) {
         float hp = mc.field_1724.method_6032();
         if (this.lastHealth < 0.0F) {
            this.lastHealth = hp;
         } else {
            long now = System.currentTimeMillis();
            if (hp < this.lastHealth - 0.05F) {
               float dmg = this.lastHealth - hp;
               class_742 near = this.nearestPlayer((double)6.0F);
               String src = near != null ? near.method_5477().getString() + " (" + near.method_6047().method_7909().method_63680().getString() + ")" : "?";
               this.log.addFirst(new Entry(String.format(Locale.ROOT, "-%.1f от %s", dmg, src), now));

               while(this.log.size() > 6) {
                  this.log.removeLast();
               }
            }

            this.log.removeIf((e) -> now - e.time() > 10000L);
            this.lastHealth = hp;
         }
      }
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList();
      if (this.log.isEmpty()) {
         lines.add("§7—");
      } else {
         for(Entry e : this.log) {
            lines.add("§c" + e.text());
         }
      }

      return RenderUtil.panel(ctx, x, y, "Damage Log", lines);
   }

   private class_742 nearestPlayer(double maxDist) {
      if (mc.field_1687 != null && mc.field_1724 != null) {
         class_742 best = null;
         double bestSq = maxDist * maxDist;

         for(class_742 p : mc.field_1687.method_18456()) {
            if (p != mc.field_1724) {
               double sq = mc.field_1724.method_5858(p);
               if (sq < bestSq) {
                  bestSq = sq;
                  best = p;
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private static record Entry(String text, long time) {
   }
}
