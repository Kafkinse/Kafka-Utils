package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.module.modules.chat.EconomyTracker;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_332;

/**
 * Draggable HUD with your balance and today's spending / income, read from the
 * {@link EconomyTracker} module (which must be enabled to record events).
 */
public class EconomyHud extends Module implements HudModule {
   public EconomyHud() {
      super("Economy HUD", "HUD с балансом и тратами за сегодня.", Category.RENDER);
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      EconomyTracker eco = ModuleManager.get(EconomyTracker.class);
      List<String> lines = new ArrayList<>();
      if (eco == null || !eco.isEnabled()) {
         lines.add("§7включи Economy Tracker");
      } else {
         String cur = eco.currency();
         lines.add("§7Баланс: §f" + (eco.balanceKnown() ? EconomyTracker.fmt(eco.balance()) : "?") + " " + cur);
         lines.add("§7Сегодня: §c-" + EconomyTracker.fmt(eco.spentToday()) + " §7/ §a+" + EconomyTracker.fmt(eco.incomeToday()));
      }
      return RenderUtil.panel(ctx, x, y, "Экономика", lines);
   }
}
