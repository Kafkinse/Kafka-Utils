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

/**
 * Draggable HUD listing only <b>your own</b> active potion effects with level and
 * remaining time (Effect Logger shows every player; this one is just you).
 */
public class PotionEffectsHud extends Module implements HudModule {
   public PotionEffectsHud() {
      super("My Effects", "HUD с твоими активными эффектами и таймерами.", Category.RENDER);
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList<>();
      if (mc.field_1724 != null) {
         for (class_1293 effect : mc.field_1724.method_6026()) {
            String name = ((class_1291) effect.method_5579().comp_349()).method_5560().getString();
            int level = effect.method_5578() + 1;
            String duration = effect.method_48559() ? "∞" : RenderUtil.time(effect.method_5584() / 20);
            lines.add("§r" + name + " " + RenderUtil.roman(level) + " §7(" + duration + ")");
         }
      }
      if (lines.isEmpty()) {
         lines.add("§7нет эффектов");
      }
      return RenderUtil.panel(ctx, x, y, "Мои эффекты", lines);
   }
}
