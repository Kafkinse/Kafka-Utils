package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_332;

public class PearlTracker extends Module implements HudModule {
   private final NumberSetting cooldownSeconds = (NumberSetting)this.add(new NumberSetting("Cooldown (s)", 20, 1, 30, 1));
   private final BooleanSetting showCooldownHud = (BooleanSetting)this.add(new BooleanSetting("Show Cooldowns", true));
   private final BooleanSetting chatLog = (BooleanSetting)this.add(new BooleanSetting("Chat Log", true));

   private final Map<UUID, PearlEntry> pearlThrows = new LinkedHashMap();
   private final Deque<String> recentLog = new ArrayDeque();

   public PearlTracker() {
      super("Pearl Tracker", "Tracks who threw ender pearls and their cooldown.", Category.COMBAT);
   }

   protected void onEnable() {
      this.pearlThrows.clear();
      this.recentLog.clear();
   }

   public void onTick() {
      if (mc.field_1687 != null && mc.field_1724 != null) {
         long now = System.currentTimeMillis();
         int cdTicks = this.cooldownSeconds.get() * 20;

         // Check for pearl throws (player holding pearl and it disappears from inventory)
         for (net.minecraft.class_742 p : mc.field_1687.method_18456()) {
            if (p != mc.field_1724) {
               UUID id = p.method_5667();
               boolean hasPearl = false;
               for (int i = 0; i < 9; i++) {
                  net.minecraft.class_1799 stack = p.method_7371().method_5438(i);
                  if (!stack.method_7960() && stack.method_7909() == net.minecraft.class_1802.field_8833) {
                     hasPearl = true;
                     break;
                  }
               }

               PearlEntry prev = this.pearlThrows.get(id);
               if (prev != null && !hasPearl && now - prev.time > 1000L) {
                  // They threw it
                  String name = p.method_5477().getString();
                  if (chatLog.get()) {
                     ChatUtil.raw("§5" + name + "§r кинул жемчуг! §dCD: " + this.cooldownSeconds.get() + "s");
                  }
                  String logEntry = "§d" + name + "§r §7" + String.format(Locale.ROOT, "%tH:%tM:%tS", now, now, now);
                  this.recentLog.addFirst(logEntry);
                  if (this.recentLog.size() > 8) {
                     this.recentLog.removeLast();
                  }
               }
               if (prev == null && hasPearl) {
                  this.pearlThrows.put(id, new PearlEntry(now));
               } else if (prev != null) {
                  prev.time = now;
               }
            }
         }

         // Remove expired
         long expireTime = now - (long)this.cooldownSeconds.get() * 1000L;
         this.pearlThrows.entrySet().removeIf(e -> {
            PearlEntry entry = e.getValue();
            if (entry.time < expireTime) {
               entry.isReady = true;
               return now - entry.time > (long)(this.cooldownSeconds.get() + 5) * 1000L;
            }
            return false;
         });
      }
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      long now = System.currentTimeMillis();
      List<String> lines = new ArrayList();
      int cd = this.cooldownSeconds.get();

      if (this.showCooldownHud.get()) {
         for (Map.Entry<UUID, PearlEntry> e : this.pearlThrows.entrySet()) {
            UUID id = e.getKey();
            PearlEntry entry = e.getValue();
            String name = this.findPlayerName(id);
            if (name == null) continue;

            long elapsed = (now - entry.time) / 1000L;
            int remaining = Math.max(0, cd - (int)elapsed);
            String color = remaining > 0 ? "§c" : "§a";
            if (entry.isReady) {
               lines.add("§d" + name + "§r: §a✓");
            } else {
               lines.add("§d" + name + "§r: " + color + remaining + "s");
            }
         }
      }

      // Recent log
      if (!this.recentLog.isEmpty()) {
         if (!lines.isEmpty()) lines.add("§7—");
         for (String log : this.recentLog) {
            lines.add("§7" + log);
         }
      }

      if (lines.isEmpty()) {
         lines.add("§7нет данных");
      }
      return RenderUtil.panel(ctx, x, y, "Pearl Tracker (CD: " + cd + "s)", lines);
   }

   private String findPlayerName(UUID id) {
      if (mc.field_1687 == null) return null;
      for (net.minecraft.class_742 p : mc.field_1687.method_18456()) {
         if (p.method_5667().equals(id)) return p.method_5477().getString();
      }
      return null;
   }

   private static class PearlEntry {
      long time;
      boolean isReady = false;
      PearlEntry(long t) { this.time = t; }
   }

   // Import helpers for the short names used
   private static net.minecraft.class_310 mc = net.minecraft.class_310.method_1551();
}
