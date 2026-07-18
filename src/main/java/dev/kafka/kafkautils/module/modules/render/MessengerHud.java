package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.module.modules.chat.Messenger;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_332;

/**
 * Draggable HUD listing chats that have unread messenger messages, with a
 * per-chat counter. Toggle it in ClickGui → Render. Your own messages never
 * count as unread (handled in {@link Messenger}).
 */
public class MessengerHud extends Module implements HudModule {
   public MessengerHud() {
      super("Messenger Unread", "HUD со счётчиком непрочитанных сообщений.", Category.RENDER);
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      Messenger m = ModuleManager.get(Messenger.class);
      List<String> lines = new ArrayList<>();
      int total = 0;
      if (m != null) {
         for (String key : m.threadKeys()) {
            int un = m.unreadOf(key);
            if (un > 0) {
               total += un;
               lines.add("§f" + key + " §c" + un);
            }
         }
      }
      if (lines.isEmpty()) {
         lines.add("§7нет непрочитанных");
      }
      return RenderUtil.panel(ctx, x, y, "✉ Непрочитанные (" + total + ")", lines);
   }
}
