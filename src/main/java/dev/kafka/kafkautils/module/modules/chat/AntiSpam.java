package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.class_2561;

public class AntiSpam extends Module {
   private final Map<String, Long> recent = new LinkedHashMap();

   public AntiSpam() {
      super("Anti Spam", "Hides duplicate chat spam.", Category.CHAT);
   }

   public boolean allow(class_2561 message) {
      if (this.isEnabled() && message != null) {
         String s = message.getString();
         long now = System.currentTimeMillis();
         Long prev = (Long)this.recent.get(s);
         this.recent.put(s, now);
         this.recent.entrySet().removeIf((e) -> now - (Long)e.getValue() > 10000L);
         return prev == null || now - prev >= 3000L;
      } else {
         return true;
      }
   }
}
