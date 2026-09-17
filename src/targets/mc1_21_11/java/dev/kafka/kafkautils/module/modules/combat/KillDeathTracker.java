package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_332;

public class KillDeathTracker extends Module implements HudModule {
   private int kills = 0;
   private int deaths = 0;
   private int killStreak = 0;
   private int bestStreak = 0;
   private boolean justDied = false;

   public KillDeathTracker() {
      super("K/D Tracker", "Tracks kills and deaths in your current session.", Category.COMBAT);
   }

   public void recordKill(String name) {
      this.kills++;
      this.killStreak++;
      if (this.killStreak > this.bestStreak) {
         this.bestStreak = this.killStreak;
      }
   }

   public void recordDeath(String killer) {
      this.deaths++;
      this.killStreak = 0;
      this.justDied = true;
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList();
      double kd = this.deaths > 0 ? (double)this.kills / (double)this.deaths : (double)this.kills;
      String kdStr = this.kills + "/" + this.deaths + " §7(§" + (kd >= 1.0 ? "a" : "c") + String.format("%.2f", kd) + "§7)";
      lines.add("§dK/D: §r" + kdStr);
      lines.add("§dStreak: §r" + this.killStreak + " §7(best: " + this.bestStreak + ")");
      if (this.justDied) {
         lines.add("§c☠ Death!");
         this.justDied = false;
      }
      return RenderUtil.panel(ctx, x, y, "Session Stats", lines);
   }
}
