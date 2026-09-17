package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.WorldRenderModule;
import dev.kafka.kafkautils.util.Render3D;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.class_1304;
import net.minecraft.class_1799;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_742;

public class ArmorDurabilityESP extends Module implements WorldRenderModule {

   public ArmorDurabilityESP() {
      super("Armor Durability ESP", "Shows armor durability above enemies' heads, visible through walls.", Category.RENDER);
   }

   public void onWorldRender(WorldRenderContext ctx) {
      if (mc.field_1687 != null && mc.field_1724 != null) {
         class_243 cameraPos = mc.field_1724.method_33571();

         for(class_742 p : mc.field_1687.method_18456()) {
            if (p != mc.field_1724) {
               class_243 pos = p.method_61411();
               double topY = pos.field_1351 + (double)p.method_17682() + 0.7;

               class_243 toPlayer = new class_243(
                  pos.field_1352 - cameraPos.field_1352, 0.0, pos.field_1350 - cameraPos.field_1350
               ).method_1029();

               double w = 0.6;
               double barH = 0.04;
               double gap = 0.05;
               double startY = topY;

               // Iterate armor slots
               for (class_1304 slot : class_1304.values()) {
                  if (!slot.method_46643()) {
                     continue;
                  }
                  class_1799 stack = p.method_6118(slot);
                  if (!stack.method_7960()) {
                     int dmg = stack.method_7919();
                     int maxDmg = getMaxDurability(stack);
                     if (maxDmg > 0) {
                        float frac = Math.max(0.0F, Math.min(1.0F, 1.0F - (float)dmg / (float)maxDmg));
                        double leftX = pos.field_1352 - w / 2.0;
                        double rightX = pos.field_1352 + w / 2.0;
                        double fillEnd = leftX + w * (double)frac;

                        class_238 bg = new class_238(leftX - 0.01, startY, pos.field_1350 - 0.03,
                                                     rightX + 0.01, startY + barH, pos.field_1350 + 0.03);
                        class_238 fg = new class_238(leftX, startY, pos.field_1350 - 0.02,
                                                     fillEnd, startY + barH, pos.field_1350 + 0.02);

                        Render3D.drawFilled(bg, -1608507356);
                        Render3D.drawFilled(fg, durColor(frac));

                        startY += barH + gap;
                     }
                  }
               }
            }
         }
      }
   }

   private static int durColor(float frac) {
      if (frac > 0.5F) {
         int g = (int)(200.0F * frac);
         return 0xFF000000 | g << 8;
      } else if (frac > 0.25F) {
         return 0xFFFFAA00;
      } else {
         return 0xFFFF4444;
      }
   }

   private static int getMaxDurability(class_1799 stack) {
      String name = stack.method_7964().getString().toLowerCase(Locale.ROOT);
      if (name.contains("netherite")) return 407;
      if (name.contains("diamond")) return 264;
      if (name.contains("iron")) return 165;
      if (name.contains("chain")) return 165;
      if (name.contains("gold")) return 77;
      if (name.contains("leather")) return 55;
      if (name.contains("turtle")) return 25;
      return 0;
   }
}
