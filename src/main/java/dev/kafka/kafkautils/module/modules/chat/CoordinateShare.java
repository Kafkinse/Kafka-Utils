package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.ChatUtil;

/**
 * Sends the player's current coordinates into chat (or a clan channel) on a
 * keybind — bind "Share Coordinates" in Options → Controls. The Prefix setting
 * lets you route it to a clan channel, e.g. "!c " or "/msg clan ".
 */
public class CoordinateShare extends Module {
   private final StringSetting prefix = this.add(new StringSetting("Prefix", ""));
   private final BooleanSetting includeDimension = this.add(new BooleanSetting("Include Dimension", true));

   public CoordinateShare() {
      super("Coordinate Share", "Sends your coordinates to chat on a keybind (see Controls).", Category.CHAT);
   }

   /** Called from the client keybind handler when the share key is pressed. */
   public void share() {
      if (mc.field_1724 == null || mc.method_1562() == null) {
         return;
      }

      int bx = mc.field_1724.method_31477();
      int by = mc.field_1724.method_31478();
      int bz = mc.field_1724.method_31479();

      StringBuilder sb = new StringBuilder();
      sb.append("X: ").append(bx).append(" Y: ").append(by).append(" Z: ").append(bz);
      if (this.includeDimension.get() && mc.field_1687 != null) {
         String dim = mc.field_1687.method_27983().method_29177().toString();
         sb.append(" (").append(dim).append(")");
      }

      String message = this.prefix.get() + sb;
      if (message.startsWith("/")) {
         mc.method_1562().method_45730(message.substring(1));
      } else {
         mc.method_1562().method_45729(message);
      }
      ChatUtil.info("Координаты отправлены");
   }
}
