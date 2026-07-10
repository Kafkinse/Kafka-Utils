package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_640;

public class PlayerTracker extends Module {
   private final StringSetting nicks = (StringSetting)this.add(new StringSetting("Nicks", ""));
   private final Set<String> seen = new HashSet();

   public PlayerTracker() {
      super("Player Tracker", "Notifies when tracked players join.", Category.CHAT);
   }

   protected void onEnable() {
      this.seen.clear();
   }

   public List<String> onlineTracked() {
      List<String> out = new ArrayList();
      if (mc.method_1562() == null) {
         return out;
      } else {
         Set<String> tracked = new HashSet();

         for(String part : this.nicks.get().split("[,\\s]+")) {
            if (!part.isBlank()) {
               tracked.add(part.trim().toLowerCase(Locale.ROOT));
            }
         }

         if (tracked.isEmpty()) {
            return out;
         } else {
            for(class_640 entry : mc.method_1562().method_2880()) {
               if (entry.method_2966() != null) {
                  String name = entry.method_2966().name();
                  if (tracked.contains(name.toLowerCase(Locale.ROOT))) {
                     out.add(name);
                  }
               }
            }

            return out;
         }
      }
   }

   protected void onDisable() {
      this.seen.clear();
   }

   public void onTick() {
      if (mc.method_1562() != null && mc.field_1724 != null) {
         Set<String> tracked = new HashSet();

         for(String part : this.nicks.get().split("[,\\s]+")) {
            if (!part.isBlank()) {
               tracked.add(part.trim().toLowerCase(Locale.ROOT));
            }
         }

         if (!tracked.isEmpty()) {
            Set<String> current = new HashSet();

            for(class_640 entry : mc.method_1562().method_2880()) {
               if (entry.method_2966() != null) {
                  String name = entry.method_2966().name();
                  String key = name.toLowerCase(Locale.ROOT);
                  current.add(key);
                  if (!this.seen.contains(key)) {
                     this.seen.add(key);
                     if (tracked.contains(key)) {
                        ChatUtil.raw("§d§l[ТРЕК] §r" + name + "§d зашёл на сервер!");
                        mc.field_1724.method_5783((class_3414)class_3417.field_14793.comp_349(), 1.0F, 1.6F);
                     }
                  }
               }
            }

            this.seen.retainAll(current);
         }
      }
   }
}
