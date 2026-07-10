package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_332;
import net.minecraft.class_742;

public class DpsMeter extends Module implements HudModule {
   private final LinkedHashMap<UUID, DamageEntry> tracking = new LinkedHashMap();
   private float lastHp = -1.0F;
   private String lastAttacker = null;

   public DpsMeter() {
      super("DPS Meter", "Measures damage per second dealt to enemies.", Category.COMBAT);
   }

   protected void onEnable() {
      this.tracking.clear();
      this.lastHp = -1.0F;
      this.lastAttacker = null;
   }

   public void onTick() {
      if (mc.field_1687 != null && mc.field_1724 != null) {
         long now = System.currentTimeMillis();
         float hp = mc.field_1724.method_6032();

         if (this.lastHp >= 0.0F && hp < this.lastHp - 0.05F) {
            float dmg = this.lastHp - hp;
            class_742 near = this.nearestPlayer(12.0);
            String name;
            if (near != null) {
               name = near.method_5477().getString();
            } else {
               name = "?";
            }
            this.lastAttacker = name;

            DamageEntry entry = this.tracking.get(name);
            if (entry == null) {
               entry = new DamageEntry();
               this.tracking.put(name, entry);
               if (this.tracking.size() > 10) {
                  Iterator<Map.Entry<UUID, DamageEntry>> it = this.tracking.entrySet().iterator();
                  it.next();
                  it.remove();
               }
            }
            entry.addHit(dmg, now);
         }
         this.lastHp = hp;

         // Cleanup old entries
         this.tracking.values().removeIf(e -> now - e.lastHit > 5000L);
      }
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      long now = System.currentTimeMillis();
      List<String> lines = new ArrayList();

      if (this.tracking.isEmpty()) {
         lines.add("§7—");
      } else {
         for (Map.Entry<String, DamageEntry> e : this.tracking.entrySet()) {
            String name = e.getKey();
            DamageEntry de = e.getValue();
            double dps = de.getDps(now);
            String marker = name.equals(this.lastAttacker) ? " §c◄" : "";
            lines.add(String.format(Locale.ROOT, "§d%s§r: §c%.1f%s", name, dps, marker));
         }
      }

      return RenderUtil.panel(ctx, x, y, "DPS Meter", lines);
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
      }
      return null;
   }

   private static class DamageEntry {
      private final List<Hit> hits = new ArrayList();
      long lastHit;

      void addHit(float dmg, long time) {
         this.hits.add(new Hit(dmg, time));
         this.lastHit = time;
         // Keep only last 5 seconds
         this.hits.removeIf(h -> time - h.time > 5000L);
      }

      double getDps(long now) {
         this.hits.removeIf(h -> now - h.time > 5000L);
         if (this.hits.isEmpty()) return 0.0;
         long first = this.hits.get(0).time;
         double span = (double)(now - first) / 1000.0;
         if (span < 0.01) return 0.0;
         double total = 0.0;
         for (Hit h : this.hits) total += h.dmg;
         return total / span;
      }

      private record Hit(float dmg, long time) {}
   }
}
