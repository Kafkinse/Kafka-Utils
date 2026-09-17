package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_332;

public class ActiveModsHud extends Module implements HudModule {
   public ActiveModsHud() {
      super("Active Mods", "Draggable list of enabled modules.", Category.RENDER);
      this.setEnabled(true);
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList();

      for(Module m : ModuleManager.getModules()) {
         if (m.isEnabled() && m != this) {
            lines.add("§d" + m.getName());
         }
      }

      if (lines.isEmpty()) {
         lines.add("§7—");
      }

      return RenderUtil.panel(ctx, x, y, "Kafka Utils", lines);
   }
}
