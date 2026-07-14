package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.setting.BooleanSetting;
import java.util.Locale;
import net.minecraft.class_2561;
import net.minecraft.class_3414;
import net.minecraft.class_3417;

/**
 * Tints chat lines that mention a clan member so clan communication stands out.
 * Uses the roster from {@link ClanList}. Wired into the client's MODIFY_GAME chat
 * pipeline alongside Chat Ping.
 */
public class ClanChatHighlight extends Module {
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));

   public ClanChatHighlight() {
      super("Clan Chat Highlight", "Colours chat lines that mention a clan member.", Category.CHAT);
   }

   public class_2561 process(class_2561 message) {
      if (!this.isEnabled() || message == null) {
         return message;
      }
      ClanList clan = ModuleManager.get(ClanList.class);
      if (clan == null) {
         return message;
      }

      String text = message.getString();
      String lower = text.toLowerCase(Locale.ROOT);
      for (String member : clan.members()) {
         if (!member.isBlank() && lower.contains(member)) {
            if (this.sound.get() && mc.field_1724 != null) {
               mc.field_1724.method_5783((class_3414)class_3417.field_14622.comp_349(), 0.7F, 1.2F);
            }
            return class_2561.method_43470("§a" + text);
         }
      }
      return message;
   }
}
