package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.ModeSetting;
import java.util.Locale;
import net.minecraft.class_2561;
import net.minecraft.class_3414;
import net.minecraft.class_3417;

/**
 * Softly marks normal chat lines that mention a friend: keeps the original
 * message and its colours and just prepends a small accent bar. Wired into the
 * MODIFY_GAME chat pipeline after Chat Ping.
 */
public class FriendHighlight extends Module {
   private final ModeSetting accent = this.add(new ModeSetting("Accent", 0, "Teal", "Green", "Aqua", "Purple", "Gold"));
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));

   public FriendHighlight() {
      super("Friend Highlight", "Softly marks chat lines that mention a friend.", Category.FRIENDS);
   }

   private String accentCode() {
      switch (this.accent.get()) {
         case "Green": return "§2";
         case "Aqua": return "§b";
         case "Purple": return "§5";
         case "Gold": return "§6";
         default: return "§3";
      }
   }

   public class_2561 process(class_2561 message) {
      if (!this.isEnabled() || message == null) {
         return message;
      }
      FriendList list = ModuleManager.get(FriendList.class);
      if (list == null) {
         return message;
      }
      String lower = message.getString().replaceAll("(?i)§[0-9A-FK-OR]", "").toLowerCase(Locale.ROOT);
      boolean mention = false;
      for (String friend : list.friends()) {
         if (!friend.isBlank() && lower.matches(".*\\b" + java.util.regex.Pattern.quote(friend) + "\\b.*")) {
            mention = true;
            break;
         }
      }
      if (mention) {
         if (this.sound.get() && mc.field_1724 != null) {
            mc.field_1724.method_5783((class_3414)class_3417.field_14622.comp_349(), 0.5F, 1.3F);
         }
         return class_2561.method_43470(this.accentCode() + "▏ §r").method_10852(message);
      }
      return message;
   }
}
