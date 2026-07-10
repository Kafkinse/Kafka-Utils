package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.util.RenderUtil;
import dev.kafka.kafkautils.util.StaffCounter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_332;

public class StaffList extends Module implements HudModule {
   public StaffList() {
      super("Staff List", "Online staff counts (Ⓗ/Ⓜ/Ⓐ) + hidden players.", Category.CHAT);
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      int[] c = StaffCounter.counts();
      List<String> lines = new ArrayList();
      lines.add("§dⒽ: §r" + c[0] + "   §dⓂ: §r" + c[1] + "   §dⒶ: §r" + c[2]);
      List<String> staff = StaffCounter.staffNames();
      if (staff.isEmpty()) {
         lines.add("§7нет стаффа онлайн");
      } else {
         for(String n : staff) {
            lines.add("§r" + n);
         }
      }

      PlayerTracker tracker = (PlayerTracker)ModuleManager.get(PlayerTracker.class);
      if (tracker != null) {
         List<String> tracked = tracker.onlineTracked();
         if (!tracked.isEmpty()) {
            lines.add("§aТрек онлайн:");

            for(String n : tracked) {
               lines.add("§a  " + n);
            }
         }
      }

      List<String> hidden = StaffCounter.hiddenPlayers();
      if (!hidden.isEmpty()) {
         lines.add("§cСкрытые/ваниш (" + hidden.size() + "):");

         for(String n : hidden) {
            lines.add("§7  " + n);
         }
      }

      return RenderUtil.panel(ctx, x, y, "Staff Online", lines);
   }
}
