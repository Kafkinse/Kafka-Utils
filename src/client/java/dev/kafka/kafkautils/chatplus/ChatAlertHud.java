package dev.kafka.kafkautils.chatplus;

import java.util.ArrayList;
import java.util.List;

/**
 * Toast-style notifications shown when a chat message mentions your name.
 * Sound playback needs a per-target Minecraft API call, so this only tracks
 * how many are pending ({@link #drainPendingSounds()}); the caller plays them.
 */
public final class ChatAlertHud {
   private static final long NOTICE_MILLIS = 4_000L;
   private static final int MAX_NOTICES = 3;
   private static final int MAX_TEXT_LENGTH = 160;
   private static final int WIDTH = 220;
   private static final int HEIGHT = 18;
   private static final int GAP = 3;
   private static final int BG = 0xD0201830;
   private static final int ACCENT = 0xFFB388FF;
   private static final int TEXT = 0xFFE7DAF6;

   private static final List<Notice> notices = new ArrayList<>();
   private static int pendingSounds;

   private ChatAlertHud() {
   }

   public static synchronized void add(String text) {
      if (text == null || text.isBlank()) {
         return;
      }
      String trimmed = text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH - 3) + "..." : text;
      notices.add(new Notice(trimmed, System.currentTimeMillis() + NOTICE_MILLIS));
      while (notices.size() > MAX_NOTICES) {
         notices.remove(0);
      }
      ++pendingSounds;
   }

   public static synchronized void tick() {
      long now = System.currentTimeMillis();
      notices.removeIf(notice -> notice.expiresAt() <= now);
   }

   /** How many alert sounds are queued since the last call; clears the counter. */
   public static synchronized int drainPendingSounds() {
      int count = pendingSounds;
      pendingSounds = 0;
      return count;
   }

   public static synchronized void render(CompatGraphics g, int guiWidth) {
      int y = 8;
      for (Notice notice : notices) {
         int x = guiWidth - WIDTH - 6;
         g.fill(x, y, x + WIDTH, y + HEIGHT, BG);
         g.outline(x, y, WIDTH, HEIGHT, ACCENT);
         g.text(notice.text(), x + 6, y + 5, TEXT);
         y += HEIGHT + GAP;
      }
   }

   private record Notice(String text, long expiresAt) {
   }
}
