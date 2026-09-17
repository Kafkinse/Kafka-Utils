package dev.kafka.kafkautils.chatplus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/** Bookmarked chat messages, persisted to disk as a pipe-delimited text file. */
public final class ChatBookmarkStore {
   private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("kafkautils-chat-bookmarks.txt");
   private static final List<ChatEntry> bookmarks = new ArrayList<>();
   private static boolean loaded;

   private ChatBookmarkStore() {
   }

   public static synchronized List<ChatEntry> all() {
      ensureLoaded();
      return new ArrayList<>(bookmarks);
   }

   public static synchronized void add(ChatEntry entry) {
      ensureLoaded();
      if (contains(entry)) {
         return;
      }
      bookmarks.add(entry);
      persist();
   }

   public static synchronized void remove(ChatEntry entry) {
      ensureLoaded();
      bookmarks.removeIf(b -> sameMessage(b, entry));
      persist();
   }

   public static synchronized boolean contains(ChatEntry entry) {
      ensureLoaded();
      for (ChatEntry bookmark : bookmarks) {
         if (sameMessage(bookmark, entry)) {
            return true;
         }
      }
      return false;
   }

   private static boolean sameMessage(ChatEntry a, ChatEntry b) {
      return a.timestamp() == b.timestamp() && a.text().equals(b.text());
   }

   private static void ensureLoaded() {
      if (loaded) {
         return;
      }
      loaded = true;
      try {
         if (!Files.exists(FILE)) {
            return;
         }
         for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
            String[] parts = line.split("\\|", 3);
            if (parts.length != 3) {
               continue;
            }
            try {
               long timestamp = Long.parseLong(parts[0]);
               ChatTab tab = ChatTab.valueOf(parts[1]);
               bookmarks.add(new ChatEntry(timestamp, decode(parts[2]), tab));
            } catch (RuntimeException ignored) {
            }
         }
      } catch (IOException ignored) {
      }
   }

   private static void persist() {
      try {
         StringBuilder builder = new StringBuilder();
         for (ChatEntry bookmark : bookmarks) {
            builder.append(bookmark.timestamp()).append('|')
                  .append(bookmark.tab().name()).append('|')
                  .append(encode(bookmark.text())).append('\n');
         }
         Files.writeString(FILE, builder.toString(), StandardCharsets.UTF_8,
               StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      } catch (IOException ignored) {
      }
   }

   private static String encode(String s) {
      return s.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n");
   }

   private static String decode(String s) {
      StringBuilder out = new StringBuilder();
      for (int i = 0; i < s.length(); ++i) {
         char c = s.charAt(i);
         if (c == '\\' && i + 1 < s.length()) {
            char next = s.charAt(++i);
            out.append(next == 'p' ? '|' : next == 'n' ? '\n' : next);
         } else {
            out.append(c);
         }
      }
      return out.toString();
   }
}
