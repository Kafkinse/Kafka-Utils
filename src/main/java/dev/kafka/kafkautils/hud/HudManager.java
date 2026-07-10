package dev.kafka.kafkautils.hud;

import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.class_310;
import net.minecraft.class_332;

public final class HudManager {
   private static final Map<String, int[]> positions = new HashMap();
   private static final Map<String, int[]> bounds = new LinkedHashMap();
   private static boolean hidden = false;

   private HudManager() {
   }

   public static void toggleHidden() {
      hidden = !hidden;
   }

   public static boolean isHidden() {
      return hidden;
   }

   public static void render(class_332 ctx) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 != null && mc.field_1687 != null) {
         if (!mc.field_1690.field_1842 && !hidden) {
            bounds.clear();
            int autoY = 5;

            for(Module m : ModuleManager.getModules()) {
               if (m.isEnabled() && m instanceof HudModule) {
                  HudModule hm = (HudModule)m;
                  String name = m.getName();
                  int[] pos = (int[])positions.get(name);
                  int x = pos != null ? pos[0] : 5;
                  int y = pos != null ? pos[1] : autoY;

                  int[] wh;
                  try {
                     wh = hm.onHudRender(ctx, x, y);
                  } catch (Throwable t) {
                     System.err.println("[KafkaUtils] HUD error in '" + name + "': " + String.valueOf(t));
                     continue;
                  }

                  if (wh == null) {
                     wh = new int[]{0, 0};
                  }

                  bounds.put(name, new int[]{x, y, wh[0], wh[1]});
                  if (pos == null) {
                     autoY += wh[1] + 4;
                  }
               }
            }

         }
      }
   }

   public static Map<String, int[]> currentBounds() {
      return bounds;
   }

   public static Map<String, int[]> getPositions() {
      return positions;
   }

   public static void setPosition(String name, int x, int y) {
      positions.put(name, new int[]{x, y});
   }

   public static void onWorldJoin() {
   }
}
