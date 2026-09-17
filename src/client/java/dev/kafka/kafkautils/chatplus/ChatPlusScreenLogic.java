package dev.kafka.kafkautils.chatplus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Layout and state for the F8 chat manager screen. Rendering and input are
 * delegated here from a small per-target Screen subclass ({@code
 * ChatPlusScreen}), so the actual screen logic is written once and shared by
 * both Minecraft targets. Every hit-test uses fixed geometry (no text-width
 * measurement) so it never has to touch a Minecraft font type directly.
 */
public final class ChatPlusScreenLogic {
   private static final int BG = 0xE6101014;
   private static final int HEADER = 0xFF1B1030;
   private static final int TAB_ON = 0xFF3A2A55;
   private static final int TAB_OFF = 0x30000000;
   private static final int TEXT = 0xFFE7DAF6;
   private static final int MUTED = 0xFF9A8FB0;
   private static final int ACCENT = 0xFFB388FF;
   private static final int TAB_W = 90;
   private static final int TAB_H = 22;
   private static final int TAB_GAP = 6;
   private static final int TAB_Y = 36;
   private static final int ROW_H = 11;
   private static final int CLOSE_W = 80;
   private static final int CLOSE_H = 20;
   private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss");

   private ChatTab selected = ChatTab.GLOBAL;
   private int scroll;
   private boolean closeRequested;

   public void render(CompatGraphics g, int mouseX, int mouseY, int screenWidth, int screenHeight) {
      g.fill(0, 0, screenWidth, screenHeight, BG);
      g.fill(0, 0, screenWidth, 30, HEADER);
      g.text("Kafka Chat+", 10, 11, ACCENT);

      ChatTab[] tabs = ChatTab.values();
      for (int i = 0; i < tabs.length; ++i) {
         int x = tabX(i);
         boolean on = tabs[i] == this.selected;
         g.fill(x, TAB_Y, x + TAB_W, TAB_Y + TAB_H, on ? TAB_ON : TAB_OFF);
         if (on) {
            g.outline(x, TAB_Y, TAB_W, TAB_H, ACCENT);
         }
         g.text(tabs[i].label(), x + 10, TAB_Y + 7, on ? TEXT : MUTED);
      }

      List<ChatEntry> entries = ChatPlusStore.entries(this.selected);
      int top = TAB_Y + TAB_H + 10;
      int bottom = screenHeight - 34;
      int rows = Math.max(1, (bottom - top) / ROW_H);
      int maxScroll = Math.max(0, entries.size() - rows);
      this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));

      if (entries.isEmpty()) {
         g.text("Пока нет сообщений на этой вкладке.", 10, top, MUTED);
      } else {
         int from = Math.max(0, entries.size() - rows - this.scroll);
         int to = Math.min(entries.size(), from + rows);
         int y = top;
         for (int i = from; i < to; ++i) {
            ChatEntry entry = entries.get(i);
            g.text(TIME.format(new Date(entry.timestamp())), 10, y, MUTED);
            g.text(entry.text(), 62, y, TEXT);
            y += ROW_H;
         }
         if (maxScroll > 0) {
            g.text("колесо — прокрутка (" + entries.size() + ")", screenWidth - 190, 12, MUTED);
         }
      }

      int cx = closeX(screenWidth);
      int cy = closeY(screenHeight);
      boolean hover = mouseX >= cx && mouseX <= cx + CLOSE_W && mouseY >= cy && mouseY <= cy + CLOSE_H;
      g.fill(cx, cy, cx + CLOSE_W, cy + CLOSE_H, hover ? TAB_ON : TAB_OFF);
      g.outline(cx, cy, CLOSE_W, CLOSE_H, MUTED);
      g.text("Закрыть", cx + 12, cy + 6, TEXT);
   }

   /** Returns true if the click hit something (the caller should treat it as consumed). */
   public boolean mouseClicked(double mouseX, double mouseY, int screenWidth, int screenHeight) {
      ChatTab[] tabs = ChatTab.values();
      for (int i = 0; i < tabs.length; ++i) {
         int x = tabX(i);
         if (mouseX >= x && mouseX <= x + TAB_W && mouseY >= TAB_Y && mouseY <= TAB_Y + TAB_H) {
            this.selected = tabs[i];
            this.scroll = 0;
            return true;
         }
      }
      int cx = closeX(screenWidth);
      int cy = closeY(screenHeight);
      if (mouseX >= cx && mouseX <= cx + CLOSE_W && mouseY >= cy && mouseY <= cy + CLOSE_H) {
         this.closeRequested = true;
         return true;
      }
      return false;
   }

   public void mouseScrolled(double amount) {
      this.scroll -= (int) Math.round(amount) * 3;
      if (this.scroll < 0) {
         this.scroll = 0;
      }
   }

   /** True once the close button has been clicked; the caller should close the screen. */
   public boolean closeRequested() {
      return this.closeRequested;
   }

   private static int tabX(int index) {
      return 10 + index * (TAB_W + TAB_GAP);
   }

   private static int closeX(int screenWidth) {
      return screenWidth - CLOSE_W - 10;
   }

   private static int closeY(int screenHeight) {
      return screenHeight - CLOSE_H - 8;
   }
}
