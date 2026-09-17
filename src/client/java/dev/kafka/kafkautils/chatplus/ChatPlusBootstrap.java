package dev.kafka.kafkautils.chatplus;

import java.util.Locale;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

/**
 * Wires up chat capture for the F8 chat manager, plus a "someone said your
 * name" toast (see {@link ChatAlertHud}). Call once from each target's mod
 * initializer, passing a lazy way to read the player's current name — this
 * needs a Minecraft API call, so it stays per-target and is only touched
 * through the supplier.
 */
public final class ChatPlusBootstrap {
   private static boolean initialised;

   private ChatPlusBootstrap() {
   }

   public static void init(Supplier<String> playerName) {
      if (initialised) {
         return;
      }
      initialised = true;

      ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, chatType, receptionTimestamp) -> {
         String text = message.getString();
         ChatPlusStore.record(ChatTab.GLOBAL, text);
         checkMention(text, playerName);
         return true;
      });
      ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
         if (!overlay) {
            String text = message.getString();
            ChatPlusStore.record(ChatTab.SYSTEM, text);
            checkMention(text, playerName);
         }
         return true;
      });
   }

   private static void checkMention(String text, Supplier<String> playerName) {
      String name = playerName.get();
      if (name == null || name.isBlank() || text == null) {
         return;
      }
      if (containsWord(text, name)) {
         ChatAlertHud.add(text);
      }
   }

   /** Case-insensitive whole-word search (won't match "Kafka" inside "Kafkaesque"). */
   private static boolean containsWord(String haystack, String word) {
      String lowerHaystack = haystack.toLowerCase(Locale.ROOT);
      String lowerWord = word.toLowerCase(Locale.ROOT);
      int index = lowerHaystack.indexOf(lowerWord);
      if (index < 0) {
         return false;
      }
      boolean leftOk = index == 0 || !Character.isLetterOrDigit(haystack.charAt(index - 1));
      int endIndex = index + word.length();
      boolean rightOk = endIndex >= haystack.length() || !Character.isLetterOrDigit(haystack.charAt(endIndex));
      return leftOk && rightOk;
   }
}
