package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.module.modules.chat.FriendList;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_332;
import net.minecraft.class_742;

/**
 * HUD panel listing friends currently loaded in the world, with their health,
 * armour and distance.
 */
public class FriendOverlay extends Module implements HudModule {
   public FriendOverlay() {
      super("Friend Overlay", "Shows friends' HP, armour and distance in a panel.", Category.FRIENDS);
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList<>();
      FriendList list = ModuleManager.get(FriendList.class);

      if (list == null || mc.field_1687 == null || mc.field_1724 == null) {
         lines.add("§7нет данных");
         return RenderUtil.panel(ctx, x, y, "Friends", lines);
      }

      for (class_742 p : mc.field_1687.method_18456()) {
         if (p == mc.field_1724 || !list.isFriend(p.method_5477().getString())) {
            continue;
         }
         int hp = Math.round(p.method_6032());
         int maxHp = Math.round(p.method_6063());
         int armor = p.method_6096();
         int dist = (int)Math.sqrt(mc.field_1724.method_5858(p));
         lines.add(String.format(Locale.ROOT, "§a%s §7HP§c%d§7/%d §7Arm§b%d §7%dm", p.method_5477().getString(), hp, maxHp, armor, dist));
      }

      if (lines.isEmpty()) {
         lines.add("§7нет друзей рядом");
      }
      return RenderUtil.panel(ctx, x, y, "Friends", lines);
   }
}
