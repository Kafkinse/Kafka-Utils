package dev.kafka.kafkautils.util;

import net.minecraft.class_2561;
import net.minecraft.class_310;

public final class ChatUtil {
   private static final String PREFIX = "§5§l[Kafka] §r";

   private ChatUtil() {
   }

   public static void info(String message) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 != null) {
         mc.field_1705.method_1743().method_1812(class_2561.method_43470("§5§l[Kafka] §r" + message));
      }
   }

   public static void raw(String message) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 != null) {
         mc.field_1705.method_1743().method_1812(class_2561.method_43470(message));
      }
   }
}
