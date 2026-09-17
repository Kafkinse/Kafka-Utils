package dev.kafka.kafkautils.chatplus;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

/**
 * Wires up chat capture for the F8 chat manager. Call once from each
 * target's mod initializer. Never hides anything — it only records what the
 * player already sees, the same way {@code EconomyTracker} observes chat.
 */
public final class ChatPlusBootstrap {
   private static boolean initialised;

   private ChatPlusBootstrap() {
   }

   public static void init() {
      if (initialised) {
         return;
      }
      initialised = true;

      ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, chatType, receptionTimestamp) -> {
         ChatPlusStore.record(ChatTab.GLOBAL, message.getString());
         return true;
      });
      ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
         if (!overlay) {
            ChatPlusStore.record(ChatTab.SYSTEM, message.getString());
         }
         return true;
      });
   }
}
