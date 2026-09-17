package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1291;
import net.minecraft.class_1293;
import net.minecraft.class_332;
import net.minecraft.class_742;

public class EffectLogger extends Module implements HudModule {
   public EffectLogger() {
      super("Effect Logger", "Shows players' active effects, level and duration.", Category.RENDER);
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList();
      if (mc.field_1687 != null) {
         for(class_742 player : mc.field_1687.method_18456()) {
            if (!player.method_6026().isEmpty()) {
               lines.add("§d§l" + player.method_5477().getString());

               for(class_1293 effect : player.method_6026()) {
                  String var10001 = this.format(effect);
                  lines.add("  " + var10001);
               }
            }
         }
      }

      if (lines.isEmpty()) {
         lines.add("§7нет эффектов");
      }

      return RenderUtil.panel(ctx, x, y, "Effect Logger", lines);
   }

   private String format(class_1293 effect) {
      String name = ((class_1291)effect.method_5579().comp_349()).method_5560().getString();
      int level = effect.method_5578() + 1;
      String duration = effect.method_48559() ? "∞" : RenderUtil.time(effect.method_5584() / 20);
      return "§r" + name + " " + RenderUtil.roman(level) + "§7 (" + duration + ")";
   }
}
