package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_239;
import net.minecraft.class_332;
import net.minecraft.class_3966;
import net.minecraft.class_742;
import net.minecraft.class_239.class_240;

public class EnemyStatus extends Module implements HudModule {
   private UUID lastUuid = null;
   private long lastSeen = 0L;

   public EnemyStatus() {
      super("Enemy Status", "Nick, HP, held item & combat state of your target.", Category.COMBAT);
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      class_1657 target = this.crosshairPlayer();
      long now = System.currentTimeMillis();
      if (target != null) {
         this.lastUuid = target.method_5667();
         this.lastSeen = now;
      } else if (this.lastUuid != null && now - this.lastSeen < 10000L) {
         target = this.findPlayer(this.lastUuid);
      }

      if (target == null) {
         return new int[]{0, 0};
      } else {
         class_1799 main = target.method_6047();
         class_1799 off = target.method_6079();
         List<String> lines = new ArrayList();
         lines.add("§d§l" + target.method_5477().getString());
         float abs = target.method_6067();
         String var10001 = fmt(target.method_6032());
         lines.add("§rHP: " + var10001 + "/" + fmt(target.method_6063()) + (abs > 0.0F ? "§d +" + fmt(abs) : ""));
         var10001 = main.method_7960() ? "—" : main.method_7964().getString();
         lines.add("§dВ руке: §r" + var10001);
         if (!off.method_7960()) {
            lines.add("§dОфф: §r" + off.method_7964().getString());
         }

         List<String> state = new ArrayList();
         if (target.method_6039()) {
            state.add("§aБлок");
         }

         if (target.method_5624()) {
            state.add("§rСпринт");
         }

         if (target.method_5715()) {
            state.add("§rSneak");
         }

         if (target.method_5809()) {
            state.add("§cГорит");
         }

         if (target.method_5771()) {
            state.add("§cЛава");
         }

         if (state.isEmpty()) {
            state.add("§7стоит");
         }

         lines.add(String.join("§r | ", state));
         if (target.method_6115()) {
            class_1799 act = target.method_6030();
            int secs = target.method_6014() / 20;
            var10001 = act.method_7909().method_63680().getString();
            lines.add("§dИсп: §r" + var10001 + "§7 (" + secs + "s)");
         }

         int[] wh = RenderUtil.panel(ctx, x, y, "Enemy", lines);
         if (!main.method_7960()) {
            ctx.method_51427(main, x + wh[0] - 20, y + 1);
         }

         if (!off.method_7960()) {
            ctx.method_51427(off, x + wh[0] - 38, y + 1);
         }

         return wh;
      }
   }

   private class_1657 crosshairPlayer() {
      if (mc.field_1765 != null && mc.field_1765.method_17783() == class_240.field_1331) {
         class_239 var2 = mc.field_1765;
         if (var2 instanceof class_3966) {
            class_3966 ehr = (class_3966)var2;
            class_1297 var3 = ehr.method_17782();
            class_1657 var10000;
            if (var3 instanceof class_1657) {
               class_1657 p = (class_1657)var3;
               var10000 = p;
            } else {
               var10000 = null;
            }

            return var10000;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private class_1657 findPlayer(UUID id) {
      if (mc.field_1687 == null) {
         return null;
      } else {
         for(class_742 p : mc.field_1687.method_18456()) {
            if (p.method_5667().equals(id)) {
               return p;
            }
         }

         return null;
      }
   }

   private static String fmt(float v) {
      return v == (float)((int)v) ? Integer.toString((int)v) : String.format(Locale.ROOT, "%.1f", v);
   }
}
