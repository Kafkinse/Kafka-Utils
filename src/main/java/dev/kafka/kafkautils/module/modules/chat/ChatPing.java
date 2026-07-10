package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.ListSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_5250;

public class ChatPing extends Module {
   private final ListSetting words = (ListSetting)this.add(new ListSetting("Words"));
   private final BooleanSetting sound = (BooleanSetting)this.add(new BooleanSetting("Sound", true));
   private final BooleanSetting highlight = (BooleanSetting)this.add(new BooleanSetting("Highlight", true));

   public ChatPing() {
      super("Chat Ping", "Pings + highlights chosen words in chat.", Category.CHAT);
   }

   public class_2561 process(class_2561 message) {
      if (this.isEnabled() && message != null) {
         List<String> active = new ArrayList();

         for(String w : this.words.values()) {
            if (w != null && !w.isBlank()) {
               active.add(w.trim());
            }
         }

         if (active.isEmpty()) {
            return message;
         } else {
            String text = message.getString();
            String lower = text.toLowerCase(Locale.ROOT);
            boolean matched = false;

            for(String w : active) {
               if (lower.contains(w.toLowerCase(Locale.ROOT))) {
                  matched = true;
                  break;
               }
            }

            if (!matched) {
               return message;
            } else {
               if (this.sound.get() && mc.field_1724 != null) {
                  mc.field_1724.method_5783((class_3414)class_3417.field_14622.comp_349(), 1.0F, 1.4F);
               }

               return !this.highlight.get() ? message : this.rebuildHighlighted(text, active);
            }
         }
      } else {
         return message;
      }
   }

   private class_2561 rebuildHighlighted(String s, List<String> active) {
      class_5250 out = class_2561.method_43473();
      String lower = s.toLowerCase(Locale.ROOT);

      int bestIdx;
      int bestLen;
      for(int i = 0; i < s.length(); i = bestIdx + bestLen) {
         bestIdx = -1;
         bestLen = 0;

         for(String w : active) {
            String lw = w.toLowerCase(Locale.ROOT);
            int idx = lower.indexOf(lw, i);
            if (idx >= 0 && (bestIdx < 0 || idx < bestIdx)) {
               bestIdx = idx;
               bestLen = lw.length();
            }
         }

         if (bestIdx < 0) {
            out.method_10852(class_2561.method_43470(s.substring(i)));
            break;
         }

         if (bestIdx > i) {
            out.method_10852(class_2561.method_43470(s.substring(i, bestIdx)));
         }

         out.method_10852(class_2561.method_43470(s.substring(bestIdx, bestIdx + bestLen)).method_27695(new class_124[]{class_124.field_1064, class_124.field_1067}));
      }

      return out;
   }
}
