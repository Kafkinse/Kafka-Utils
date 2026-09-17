package dev.kafka.kafkautils.chatplus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * In-memory ring buffer of captured chat messages. Shared by both Minecraft
 * targets; nothing here touches a Minecraft API, so it compiles unchanged
 * against either mapping scheme.
 */
public final class ChatPlusStore {
   private static final int CAPACITY = 500;
   private static final Deque<ChatEntry> entries = new ArrayDeque<>();

   private ChatPlusStore() {
   }

   public static synchronized void record(ChatTab tab, String text) {
      if (text == null || text.isBlank()) {
         return;
      }
      entries.addLast(new ChatEntry(System.currentTimeMillis(), text, tab));
      while (entries.size() > CAPACITY) {
         entries.removeFirst();
      }
   }

   public static synchronized List<ChatEntry> entries(ChatTab tab) {
      List<ChatEntry> result = new ArrayList<>();
      for (ChatEntry entry : entries) {
         if (tab == null || entry.tab() == tab) {
            result.add(entry);
         }
      }
      return result;
   }

   public static synchronized void clear() {
      entries.clear();
   }
}
