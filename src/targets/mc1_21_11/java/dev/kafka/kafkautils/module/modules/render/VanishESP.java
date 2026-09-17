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
import net.minecraft.class_332;
import net.minecraft.class_742;

public class VanishESP extends Module implements WorldRenderModule, HudModule {
   private final List<String> found = new ArrayList();

   public VanishESP() {
      super("Vanish ESP", "Reveals invisible/vanished players (box + list).", Category.RENDER);
   }

   public void onWorldRender(WorldRenderContext ctx) {
      this.found.clear();
      if (mc.field_1687 != null && mc.field_1724 != null) {
         for(class_742 p : mc.field_1687.method_18456()) {
            if (p != mc.field_1724 && !p.method_5667().equals(mc.field_1724.method_5667()) && p.method_5767()) {
               Render3D.drawBox(p.method_5829(), -13388289);
               this.found.add(p.method_5477().getString());
            }
         }

      }
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList();
      if (this.found.isEmpty()) {
         lines.add("§7никого");
      } else {
         for(String n : this.found) {
            lines.add("§d" + n);
         }
      }

      return RenderUtil.panel(ctx, x, y, "Vanish (" + this.found.size() + ")", lines);
   }
}
