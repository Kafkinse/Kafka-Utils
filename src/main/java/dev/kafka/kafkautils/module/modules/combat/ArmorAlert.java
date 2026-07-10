package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_332;
import net.minecraft.class_742;

public class ArmorAlert extends Module implements HudModule {
   private final NumberSetting thresholdPercent = (NumberSetting)this.add(new NumberSetting("Threshold %", 15, 1, 100, 5));
   private final BooleanSetting showInHud = (BooleanSetting)this.add(new BooleanSetting("Show in HUD", true));
   private final BooleanSetting chatAlert = (BooleanSetting)this.add(new BooleanSetting("Chat Alert", true));

   private final Map<UUID, String[]> warnedPieces = new HashMap();

   public ArmorAlert() {
      super("Armor Alert", "Warns when enemy armor is close to breaking.", Category.COMBAT);
   }

   protected void onEnable() {
      this.warnedPieces.clear();
   }

   public void onTick() {
      if (mc.field_1687 != null && mc.field_1724 != null) {
         for(class_742 p : mc.field_1687.method_18456()) {
            if (p != mc.field_1724) {
               UUID id = p.method_5667();
               List<String> low = new ArrayList();
               for (net.minecraft.class_1799 stack : p.method_6136()) {
                  if (!stack.method_7960() && stack.method_7909().method_63680().method_63700().contains("armor")) {
                     int dmg = stack.method_7929();
                     int maxDmg = stack.method_7909().method_63680().method_63700().contains("netherite") ? 407
                        : stack.method_7909().method_63680().method_63700().contains("diamond") ? 264
                        : stack.method_7909().method_63680().method_63700().contains("iron") ? 165
                        : stack.method_7909().method_63680().method_63700().contains("chain") ? 165
                        : stack.method_7909().method_63680().method_63700().contains("gold") ? 77
                        : 0;
                     if (maxDmg > 0) {
                        int maxDura = maxDmg;
                        int curDura = maxDmg - dmg;
                        float pct = (float)curDura / (float)maxDura * 100.0F;
                        if (pct <= (float)this.thresholdPercent.get()) {
                           low.add(stack.method_7964().getString());
                        }
                     }
                  }
               }
               this.warnedPieces.put(id, low.toArray(new String[0]));
            }
         }
      }
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList();
      if (mc.field_1687 != null && mc.field_1724 != null) {
         for(class_742 p : mc.field_1687.method_18456()) {
            if (p != mc.field_1724) {
               String[] low = this.warnedPieces.get(p.method_5667());
               if (low != null && low.length > 0) {
                  lines.add("§c" + p.method_5477().getString() + " §7— " + String.join("§7, §c", low));
               }
            }
         }
      }
      if (lines.isEmpty()) {
         lines.add("§7броня в порядке");
      }
      return RenderUtil.panel(ctx, x, y, "Armor Alert (≤" + this.thresholdPercent.get() + "%)", lines);
   }
}
