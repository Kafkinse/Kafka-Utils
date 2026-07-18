package dev.kafka.kafkautils.hud;

import dev.kafka.kafkautils.config.ConfigManager;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.class_1041;
import net.minecraft.class_310;
import net.minecraft.class_312;
import net.minecraft.class_332;
import net.minecraft.class_408;
import org.lwjgl.glfw.GLFW;

public final class HudManager {
   private static final Map<String, int[]> positions = new HashMap();
   private static final Map<String, int[]> bounds = new LinkedHashMap();
   private static boolean hidden = false;
   private static String dragging;
   private static int dragOffX;
   private static int dragOffY;
   private static boolean wasDown;

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

   /**
    * Lets HUD panels be dragged with the mouse while the chat screen is open.
    * Polled each client tick; reads the raw mouse + left button and moves the
    * hovered panel, saving positions when the drag ends.
    */
   public static void tickChatDrag() {
      class_310 mc = class_310.method_1551();
      if (!(mc.field_1755 instanceof class_408)) {
         dragging = null;
         wasDown = false;
         return;
      }
      class_1041 w = mc.method_22683();
      boolean down = GLFW.glfwGetMouseButton(w.method_4490(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
      double mx = mc.field_1729.method_1603() * w.method_4486() / (double)w.method_4480();
      double my = mc.field_1729.method_1604() * w.method_4502() / (double)w.method_4507();

      if (down && !wasDown) {
         for (Map.Entry<String, int[]> e : bounds.entrySet()) {
            int[] b = e.getValue();
            if (b[2] > 0 && b[3] > 0 && mx >= b[0] && mx <= b[0] + b[2] && my >= b[1] && my <= b[1] + b[3]) {
               dragging = e.getKey();
               dragOffX = (int)mx - b[0];
               dragOffY = (int)my - b[1];
               break;
            }
         }
      } else if (down && dragging != null) {
         int nx = Math.max(0, Math.min(w.method_4486() - 20, (int)mx - dragOffX));
         int ny = Math.max(0, Math.min(w.method_4502() - 10, (int)my - dragOffY));
         setPosition(dragging, nx, ny);
      } else if (!down) {
         if (dragging != null) {
            ConfigManager.save();
            dragging = null;
         }
      }
      wasDown = down;
   }
}
