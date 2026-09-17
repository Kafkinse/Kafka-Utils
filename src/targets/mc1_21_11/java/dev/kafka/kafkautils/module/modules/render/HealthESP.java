package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.WorldRenderModule;
import dev.kafka.kafkautils.util.Render3D;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_742;

public class HealthESP extends Module implements WorldRenderModule {
   public HealthESP() {
      super("Health ESP", "HP & Armor bars above players — visible through walls, always facing you.", Category.RENDER);
   }

   public void onWorldRender(WorldRenderContext ctx) {
      if (mc.field_1687 != null && mc.field_1724 != null) {
         class_243 cameraPos = mc.field_1724.method_33571();

         for(class_742 p : mc.field_1687.method_18456()) {
            if (p != mc.field_1724) {
               float maxHp = p.method_6063();
               if (maxHp <= 0.0F) continue;

               float hpFrac = Math.max(0.0F, Math.min(1.0F, p.method_6032() / maxHp));
               class_243 pos = p.method_61411();
               double topY = pos.field_1351 + (double)p.method_17682() + 0.5;

               // Calculate direction from camera to player for billboard effect
               class_243 toPlayer = new class_243(
                  pos.field_1352 - cameraPos.field_1352,
                  0.0,
                  pos.field_1350 - cameraPos.field_1350
               ).method_1029();

               double w = 0.9;
               double h = 0.12;
               double depth = 0.02;

               // Health bar - always facing camera (billboard)
               class_243 right = new class_243(-toPlayer.field_1350, 0.0, toPlayer.field_1352);
               class_243 up = new class_243(0.0, 1.0, 0.0);

               double hx = right.field_1352 * w / 2.0;
               double hz = right.field_1350 * w / 2.0;

               // Background bar
               class_243 bg1 = new class_243(pos.field_1352 - hx, topY - depth, pos.field_1350 - hz);
               class_243 bg2 = new class_243(pos.field_1352 + hx, topY + depth, pos.field_1350 + hz);
               class_243 bg3 = new class_243(pos.field_1352 - hx, topY + h + depth, pos.field_1350 - hz);
               class_243 bg4 = new class_243(pos.field_1352 + hx, topY + h + depth, pos.field_1350 + hz);

               // Simplified: draw health bar as a flat rectangle
               double leftX = pos.field_1352 - hx;
               double rightX = pos.field_1352 + hx;
               double bottomY = topY;
               double topYB = topY + h;

               class_238 bg = new class_238(leftX - 0.02, bottomY, pos.field_1350 - hz - 0.02,
                                            rightX + 0.02, topYB + 0.02, pos.field_1350 + hz + 0.02);
               double fillEnd = leftX + (rightX - leftX) * (double)hpFrac;
               class_238 fg = new class_238(leftX, bottomY, pos.field_1350 - hz,
                                            fillEnd, topYB, pos.field_1350 + hz);

               Render3D.drawFilled(bg, -1608507356);
               Render3D.drawFilled(fg, hpColor(hpFrac));

               // Armor bar below health
               float armor = p.method_6096();
               if (armor > 0.0F) {
                  float maxArmor = 20.0F;
                  float armorFrac = Math.min(1.0F, armor / maxArmor);
                  double armorY = bottomY - 0.15;
                  double armorH = 0.08;

                  class_238 armorBg = new class_238(leftX - 0.02, armorY - 0.02, pos.field_1350 - hz - 0.02,
                                                     rightX + 0.02, armorY + armorH + 0.02, pos.field_1350 + hz + 0.02);
                  double armorFillEnd = leftX + (rightX - leftX) * (double)armorFrac;
                  class_238 armorFg = new class_238(leftX, armorY, pos.field_1350 - hz,
                                                     armorFillEnd, armorY + armorH, pos.field_1350 + hz);

                  Render3D.drawFilled(armorBg, -1608507356);
                  Render3D.drawFilled(armorFg, armorColor(armorFrac));
               }
            }
         }
      }
   }

   private static int hpColor(float f) {
      if (f > 0.5F) {
         int r = (int)(255.0F * (1.0F - f) * 2.0F);
         return -16777216 | r << 16 | 170 << 8;
      } else {
         int g = (int)(255.0F * f * 2.0F);
         return -16777216 | 255 << 16 | g << 8;
      }
   }

   private static int armorColor(float f) {
      int b = (int)(200.0F * f);
      return -16777216 | b | 170 << 8;
   }
}
