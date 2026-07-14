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
 * Marks clan communication in chat so it stands out without being harsh: it keeps
 * the original message (and its colours) intact and just prepends a soft accent
 * bar. Triggers on clan-channel lines (the "(Клан)" prefix) and on lines that
 * mention a clan member from {@link ClanList}. Wired into the MODIFY_GAME chat
 * pipeline after Chat Ping.
 */
public class ClanChatHighlight extends Module {
   private final ModeSetting accent = this.add(new ModeSetting("Accent", 0, "Teal", "Green", "Aqua", "Purple", "Gold"));
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));

   public ClanChatHighlight() {
      super("Clan Chat Highlight", "Softly marks clan-chat and clan-member messages.", Category.CLAN);
   }

   private String accentCode() {
      switch (this.accent.get()) {
         case "Green": return "§2";
         case "Aqua": return "§b";
         case "Purple": return "§5";
         case "Gold": return "§6";
         default: return "§3"; // Teal
      }
   }

   public class_2561 process(class_2561 message) {
      if (!this.isEnabled() || message == null) {
         return message;
      }

      String text = message.getString().replaceAll("(?i)§[0-9A-FK-OR]", "");
      String lower = text.toLowerCase(Locale.ROOT);

      boolean clanChat = lower.contains("(клан)") || lower.contains("[клан]");
      boolean mention = false;
      if (!clanChat) {
         ClanList clan = ModuleManager.get(ClanList.class);
         if (clan != null) {
            for (String member : clan.members()) {
               if (!member.isBlank() && lower.contains(member)) {
                  mention = true;
                  break;
               }
            }
         }
      }

      if (clanChat || mention) {
         if (this.sound.get() && mc.field_1724 != null) {
            mc.field_1724.method_5783((class_3414)class_3417.field_14622.comp_349(), 0.5F, 1.3F);
         }
         // Keep the original message (colours and all) and prepend a soft accent bar.
         return class_2561.method_43470(this.accentCode() + "▏ §r").method_10852(message);
      }
      return message;
   }
}
