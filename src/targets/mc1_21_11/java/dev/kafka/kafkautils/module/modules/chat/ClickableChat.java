package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.class_2558;
import net.minecraft.class_2561;
import net.minecraft.class_2568;
import net.minecraft.class_2583;
import net.minecraft.class_5250;

/**
 * Makes chat interactive: click a whisper to reply (fills {@code /msg <name> }),
 * and click a line that mentions a command like {@code /warp}, {@code /tpaccept}
 * to run it. Rewrites the incoming message's style with click/hover events; the
 * original text and colours are preserved.
 */
public class ClickableChat extends Module {
   private final BooleanSetting reply = this.add(new BooleanSetting("Reply on Whisper", true));
   private final BooleanSetting commandsOn = this.add(new BooleanSetting("Clickable Commands", true));
   private final StringSetting commands = this.add(new StringSetting("Run Commands",
      "warp,pwarp,tpaccept,tpdeny,tpacancel,tpyes,tpno,spawn,home"));

   public ClickableChat() {
      super("Clickable Chat", "Клик по личке — быстрый ответ, клик по /warp, /tpaccept и т.п. — выполнить.", Category.CHAT);
   }

   /** Adds click/hover actions to an incoming chat line (or returns it unchanged). */
   public class_2561 process(class_2561 message) {
      if (!this.isEnabled() || message == null) {
         return message;
      }
      String plain = message.getString();

      if (this.reply.get()) {
         String target = this.whisperTarget(plain);
         if (target != null) {
            return this.withActions(message,
               new class_2558.class_10610("/msg " + target + " "),
               "§7Нажми, чтобы ответить §r" + target);
         }
      }

      if (this.commandsOn.get()) {
         String cmd = this.detectCommand(plain);
         if (cmd != null) {
            return this.withActions(message,
               new class_2558.class_10609(cmd),
               "§7Нажми, чтобы выполнить §a" + cmd);
         }
      }

      return message;
   }

   private class_2561 withActions(class_2561 message, class_2558 click, String hover) {
      class_2583 style = message.method_10866()
         .method_10958(click)
         .method_10949(new class_2568.class_10613(class_2561.method_43470(hover)));
      return message.method_27661().method_10862(style);
   }

   /** For an "A -> B" whisper, returns the other participant (whoever is not you). */
   private String whisperTarget(String plain) {
      int i = plain.indexOf("->");
      if (i <= 0) {
         return null;
      }
      String left = clean(lastWord(plain.substring(0, i)));
      String right = clean(firstWord(plain.substring(i + 2)));
      if (left.isEmpty() || right.isEmpty()) {
         return null;
      }
      String me = mc.field_1724 != null ? mc.field_1724.method_5477().getString() : null;
      if (me != null) {
         if (left.equalsIgnoreCase(me)) {
            return right;
         }
         if (right.equalsIgnoreCase(me)) {
            return left;
         }
      }
      return left; // fallback: reply to the sender
   }

   private String detectCommand(String plain) {
      for (String c : this.commands.get().split("[,\\s]+")) {
         String cmd = c.trim().toLowerCase(Locale.ROOT);
         if (cmd.isEmpty()) {
            continue;
         }
         Matcher m = Pattern.compile("/" + Pattern.quote(cmd) + "(?:\\s+\\S+)?").matcher(plain.toLowerCase(Locale.ROOT));
         if (m.find()) {
            return plain.substring(m.start(), m.end()).trim();
         }
      }
      return null;
   }

   private static String firstWord(String s) {
      String t = s.trim();
      int sp = indexOfWhitespace(t);
      return sp < 0 ? t : t.substring(0, sp);
   }

   private static String lastWord(String s) {
      String t = s.trim();
      int sp = lastIndexOfWhitespace(t);
      return sp < 0 ? t : t.substring(sp + 1);
   }

   private static int indexOfWhitespace(String s) {
      for (int i = 0; i < s.length(); ++i) {
         if (Character.isWhitespace(s.charAt(i))) {
            return i;
         }
      }
      return -1;
   }

   private static int lastIndexOfWhitespace(String s) {
      for (int i = s.length() - 1; i >= 0; --i) {
         if (Character.isWhitespace(s.charAt(i))) {
            return i;
         }
      }
      return -1;
   }

   /** Strips surrounding punctuation/brackets so a rank prefix or "name:" resolves cleanly. */
   private static String clean(String s) {
      return s.replaceAll("^[^A-Za-z0-9_]+", "").replaceAll("[^A-Za-z0-9_]+$", "");
   }
}
