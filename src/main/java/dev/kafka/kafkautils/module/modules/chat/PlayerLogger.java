package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.ChatUtil;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.class_640;

public class PlayerLogger extends Module {
   private final Set<String> known = new HashSet();

   public PlayerLogger() {
      super("Player Logger", "Logs players joining/leaving the server.", Category.CHAT);
   }

   protected void onEnable() {
      this.known.clear();
      if (mc.method_1562() != null) {
         for(class_640 entry : mc.method_1562().method_2880()) {
            if (entry.method_2966() != null) {
               this.known.add(entry.method_2966().name());
            }
         }
      }

   }

   protected void onDisable() {
      this.known.clear();
   }

   public void onTick() {
      if (mc.method_1562() != null) {
         Set<String> current = new HashSet();

         for(class_640 entry : mc.method_1562().method_2880()) {
            if (entry.method_2966() != null) {
               String name = entry.method_2966().name();
               current.add(name);
               if (!this.known.contains(name)) {
                  ChatUtil.raw("§a+ §r" + name + "§a зашёл");
               }
            }
         }

         for(String name : this.known) {
            if (!current.contains(name)) {
               ChatUtil.raw("§c- §r" + name + "§c вышел");
            }
         }

         this.known.clear();
         this.known.addAll(current);
      }
   }
}
