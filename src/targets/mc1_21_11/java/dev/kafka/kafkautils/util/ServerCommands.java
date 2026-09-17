package dev.kafka.kafkautils.util;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.class_310;

/**
 * Command syntax for vanilla-box.ru, with {player}/{amount}/{message}
 * placeholders. The exact syntax isn't discoverable from server chat output
 * alone (EconomyTracker only reads what the server prints back, not what a
 * client is supposed to send) — these come from CNDL_chat+'s own published
 * default command templates for this same server.
 */
public final class ServerCommands {
   private static final Map<String, String> TEMPLATES = new LinkedHashMap<>();

   static {
      TEMPLATES.put("pay", "pay {player} {amount}");
      TEMPLATES.put("msg", "w {player} {message}");
      TEMPLATES.put("mail", "mail send {player} {message}");
      TEMPLATES.put("call", "call {player}");
      TEMPLATES.put("tpaccept", "tpaccept");
      TEMPLATES.put("ignore", "ignoreplayer {player}");
      TEMPLATES.put("lookup", "clan lookup {player}");
      TEMPLATES.put("protection_add", "ps add {player}");
      TEMPLATES.put("protection_remove", "ps remove {player}");
      TEMPLATES.put("trader_trust", "vm trusted add {player}");
      TEMPLATES.put("trader_untrust", "vm trusted remove {player}");
      TEMPLATES.put("claimfly", "claimfly");
      TEMPLATES.put("enderchest", "enderchest");
      TEMPLATES.put("marry_kiss", "marry kiss");
      TEMPLATES.put("marry_home", "marry home");
      TEMPLATES.put("marry_tp", "marry tp");
   }

   private ServerCommands() {
   }

   /** Fills in whichever of {player}/{amount}/{message} the template uses and sends it. Any unused
    * param can be null. Returns false if there's no active connection to send through. */
   public static boolean send(String key, String player, String amount, String message) {
      String template = TEMPLATES.get(key);
      if (template == null) {
         return false;
      }
      String command = template;
      if (player != null) {
         command = command.replace("{player}", player);
      }
      if (amount != null) {
         command = command.replace("{amount}", amount);
      }
      if (message != null) {
         command = command.replace("{message}", message);
      }
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 == null) {
         return false;
      }
      mc.method_1562().method_45730(command);
      return true;
   }
}
