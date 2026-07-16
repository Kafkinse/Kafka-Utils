package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_332;

/**
 * A HUD panel that shows a pinned order (an enchant plan or a brewing recipe)
 * chosen from the helper menus. Pinning enables the module and stores the lines;
 * unpinning clears and disables it. It is draggable via the HUD editor like any
 * other HUD element.
 */
public class PinnedOrder extends Module implements HudModule {
   private List<String> lines = new ArrayList<>();
   private String title = "Порядок";

   public PinnedOrder() {
      super("Pinned Order", "Закреплённый на экране порядок (зачарование/варка).", Category.RENDER);
   }

   public void pin(String title, List<String> lines) {
      this.title = title;
      this.lines = new ArrayList<>(lines);
      this.setEnabled(true);
   }

   public void unpin() {
      this.lines = new ArrayList<>();
      this.setEnabled(false);
   }

   public boolean isPinned() {
      return this.isEnabled() && !this.lines.isEmpty();
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      if (this.lines.isEmpty()) {
         return new int[]{0, 0};
      }
      return RenderUtil.panel(ctx, x, y, this.title, this.lines);
   }
}
