package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.WorldRenderModule;
import dev.kafka.kafkautils.util.Render3D;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.class_1923;
import net.minecraft.class_2338;
import net.minecraft.class_2586;
import net.minecraft.class_2791;
import net.minecraft.class_2818;
import net.minecraft.class_332;
import net.minecraft.class_8961;

public class TrialSpawnerEsp extends Module implements WorldRenderModule, HudModule {
   private static final int SCAN_INTERVAL = 20;
   private static final int MAX_RADIUS = 8;
   private final List<class_2338> spawners = new ArrayList();
   private int scanTimer = 0;

   public TrialSpawnerEsp() {
      super("Trial Spawner ESP", "Highlights Trial Spawners through walls.", Category.RENDER);
   }

   protected void onEnable() {
      this.scanTimer = 0;
      this.spawners.clear();
   }

   protected void onDisable() {
      this.spawners.clear();
   }

   public void onTick() {
      if (mc.field_1687 != null && mc.field_1724 != null) {
         if (this.scanTimer-- <= 0) {
            this.scanTimer = 20;
            this.rescan();
         }
      }
   }

   private void rescan() {
      this.spawners.clear();
      int radius = Math.min(8, (Integer)mc.field_1690.method_42503().method_41753());
      class_1923 center = mc.field_1724.method_31476();

      for(int cx = center.field_9181 - radius; cx <= center.field_9181 + radius; ++cx) {
         for(int cz = center.field_9180 - radius; cz <= center.field_9180 + radius; ++cz) {
            class_2791 chunk = mc.field_1687.method_8497(cx, cz);
            if (chunk instanceof class_2818 worldChunk) {
               for(class_2586 be : worldChunk.method_12214().values()) {
                  if (be instanceof class_8961) {
                     this.spawners.add(be.method_11016().method_10062());
                  }
               }
            }
         }
      }

   }

   public void onWorldRender(WorldRenderContext ctx) {
      for(class_2338 pos : this.spawners) {
         Render3D.drawBox(pos, -6606593);
      }

   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList();
      lines.add("§dНайдено: §r" + this.spawners.size());
      return RenderUtil.panel(ctx, x, y, "Trial Spawners", lines);
   }
}
