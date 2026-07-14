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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_1304;
import net.minecraft.class_1799;
import net.minecraft.class_332;
import net.minecraft.class_742;

public class ArmorAlert extends Module implements HudModule {
   private static final class_1304[] ARMOR_SLOTS = {class_1304.HEAD, class_1304.CHEST, class_1304.LEGS, class_1304.FEET};
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
               for (class_1304 slot : ARMOR_SLOTS) {
                  class_1799 stack = p.method_6118(slot);
                  if (stack.method_7960()) {
                     continue;
                  }
                  String name = stack.method_7964().getString().toLowerCase(Locale.ROOT);
                  int maxDmg = name.contains("netherite") ? 407
                     : name.contains("diamond") ? 264
                     : name.contains("iron") ? 165
                     : name.contains("chain") ? 165
                     : name.contains("gold") ? 77
                     : 0;
                  if (maxDmg > 0) {
                     int curDura = maxDmg - stack.method_7919();
                     float pct = (float)curDura / (float)maxDmg * 100.0F;
                     if (pct <= (float)this.thresholdPercent.get()) {
                        low.add(stack.method_7964().getString());
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
