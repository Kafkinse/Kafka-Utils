package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.WorldRenderModule;
import dev.kafka.kafkautils.util.Render3D;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_742;

public class SplashPredictor extends Module implements WorldRenderModule {
   private static final float RADIUS = 4.0F;
   private static final int RED = -53200;

   public SplashPredictor() {
      super("Splash Predictor", "Draws splash-potion radius enemies are holding.", Category.COMBAT);
   }

   public void onWorldRender(WorldRenderContext ctx) {
      if (mc.field_1687 != null && mc.field_1724 != null) {
         for(class_742 p : mc.field_1687.method_18456()) {
            if (p != mc.field_1724) {
               class_1792 it = p.method_6047().method_7909();
               if (it == class_1802.field_8436 || it == class_1802.field_8150) {
                  boolean inRange = mc.field_1724.method_5858(p) <= (double)16.0F;
                  Render3D.circle(p.method_73189(), 4.0F, inRange ? -53200 : -29696);
               }
            }
         }

      }
   }
}
