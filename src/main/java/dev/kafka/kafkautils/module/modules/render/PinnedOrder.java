package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.class_332;

/**
 * A HUD panel that shows a pinned order (an enchant plan or a brewing recipe)
 * chosen from the helper menus. It stays live: the lines are recomputed from a
 * supplier a few times a second, so an enchant preset updates on its own as you
 * pick up the item and the missing books. Draggable via the HUD editor.
 */
public class PinnedOrder extends Module implements HudModule {
   private Supplier<List<String>> provider;
   private List<String> lines = new ArrayList<>();
   private String title = "Порядок";
   private long lastCompute;

   public PinnedOrder() {
      super("Pinned Order", "Закреплённый на экране порядок (зачарование/варка), обновляется сам.", Category.RENDER);
   }

   public void pin(String title, Supplier<List<String>> provider) {
      this.title = title;
      this.provider = provider;
      this.lines = safeGet(provider);
      this.lastCompute = System.currentTimeMillis();
      this.setEnabled(true);
   }

   public void unpin() {
      this.provider = null;
      this.lines = new ArrayList<>();
      this.setEnabled(false);
   }

   public boolean isPinned() {
      return this.isEnabled() && this.provider != null;
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      if (this.provider == null) {
         return new int[]{0, 0};
      }
      long now = System.currentTimeMillis();
      if (now - this.lastCompute > 400L) {
         this.lines = safeGet(this.provider);
         this.lastCompute = now;
      }
      if (this.lines.isEmpty()) {
         return new int[]{0, 0};
      }
      return RenderUtil.panel(ctx, x, y, this.title, this.lines);
   }

   private static List<String> safeGet(Supplier<List<String>> p) {
      try {
         List<String> l = p.get();
         return l != null ? l : new ArrayList<>();
      } catch (Throwable t) {
         return new ArrayList<>();
      }
   }
}
