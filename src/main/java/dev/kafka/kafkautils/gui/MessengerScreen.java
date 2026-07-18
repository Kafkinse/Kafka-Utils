package dev.kafka.kafkautils.gui;

import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.module.modules.chat.Messenger;
import dev.kafka.kafkautils.util.RelayClient;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_11908;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_640;
import net.minecraft.class_7532;
import org.lwjgl.glfw.GLFW;

/**
 * Discord-style messenger window: threads on the left (groups #, DMs @ with
 * unread badges), messages on the right with the sender's head avatar and the
 * name above the bubble, an input box and a send button. Opened with /kafka pm.
 */
public class MessengerScreen extends class_437 {
   private static final int BG = 0xE6101014;
   private static final int HEADER = 0xFF1B1030;
   private static final int ACCENT = 0xFFB388FF;
   private static final int PANEL = 0x33000000;
   private static final int LEFT_W = 150;
   private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

   private static String selected;

   private final Messenger msg;
   private class_342 input;
   private class_342 dmField;
   private String note = "";
   private int scroll; // blocks scrolled up from the newest (0 = latest)

   public MessengerScreen() {
      super(class_2561.method_43470("Мессенджер"));
      this.msg = ModuleManager.get(Messenger.class);
   }

   protected void method_25426() {
      if (this.msg == null) {
         return;
      }

      // Left: thread tabs.
      int y = 40;
      for (String key : this.msg.threadKeys()) {
         final String k = key;
         int un = this.msg.unreadOf(key);
         String label = (k.equals(selected) ? "§d§l" : "§7") + key + (un > 0 ? " §c(" + un + ")" : "");
         this.method_37063(class_4185.method_46430(class_2561.method_43470(label), (b) -> {
            selected = k;
            this.scroll = 0;
            this.msg.openThread(k);
            this.method_41843();
         }).method_46434(10, y, LEFT_W - 6, 14).method_46431());
         y += 16;
      }

      // Left bottom: name field + actions (open DM, create group, add member).
      int nameY = this.field_22790 - 62;
      this.dmField = new class_342(this.field_22793, 10, nameY, LEFT_W - 6, 14, class_2561.method_43470("ник / группа"));
      this.dmField.method_1880(24);
      this.dmField.method_47404(class_2561.method_43470("§7ник / группа…"));
      this.method_37063(this.dmField);

      int actY = this.field_22790 - 44;
      this.method_37063(class_4185.method_46430(class_2561.method_43470("§aЛС"), (b) -> this.doOpenDm())
         .method_46434(10, actY, 44, 14).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470("§bГруппа"), (b) -> this.doCreateGroup())
         .method_46434(58, actY, 54, 14).method_46431());
      if (selected != null && selected.startsWith("#")) {
         this.method_37063(class_4185.method_46430(class_2561.method_43470("§d+уч"), (b) -> this.doAddMember())
            .method_46434(116, actY, 34, 14).method_46431());
      }

      // Bottom: message input + send.
      int inY = this.field_22790 - 24;
      this.input = new class_342(this.field_22793, LEFT_W + 12, inY, this.field_22789 - LEFT_W - 100, 16, class_2561.method_43470("Сообщение"));
      this.input.method_1880(256);
      this.input.method_47404(class_2561.method_43470("§7Сообщение…"));
      this.method_37063(this.input);
      this.method_37063(class_4185.method_46430(class_2561.method_43470("§aОтправить"), (b) -> this.doSend())
         .method_46434(this.field_22789 - 82, inY, 72, 16).method_46431());

      // Put the cursor in the message box right away — no click needed.
      this.method_48265(this.input);
   }

   /** Enter sends the current message (or opens the DM if the name box is active). */
   public boolean method_25404(class_11908 key) {
      int code = key.comp_4795();
      if (code == GLFW.GLFW_KEY_ENTER || code == GLFW.GLFW_KEY_KP_ENTER) {
         if (this.dmField != null && this.dmField.method_25370() && !this.dmField.method_1882().isBlank()) {
            this.doOpenDm();
         } else {
            this.doSend();
         }
         return true;
      }
      return super.method_25404(key);
   }

   /** Wheel scrolls the message history of the open thread. */
   public boolean method_25401(double mouseX, double mouseY, double horiz, double vert) {
      if (selected != null && vert != 0) {
         this.scroll += (int) Math.round(vert) * 3;
         if (this.scroll < 0) {
            this.scroll = 0;
         }
         return true;
      }
      return super.method_25401(mouseX, mouseY, horiz, vert);
   }

   private void doOpenDm() {
      if (this.msg == null) {
         return;
      }
      String n = this.dmField.method_1882().trim();
      if (n.startsWith("@") || n.startsWith("#")) {
         n = n.substring(1);
      }
      if (n.isEmpty()) {
         return;
      }
      selected = "@" + n;
      this.scroll = 0;
      this.msg.messages(selected); // create the thread
      this.msg.openThread(selected);
      this.dmField.method_1852("");
      this.rebuildKeepingDraft();
   }

   private void doCreateGroup() {
      if (this.msg == null) {
         return;
      }
      String n = this.dmField.method_1882().trim();
      if (n.startsWith("#") || n.startsWith("@")) {
         n = n.substring(1);
      }
      if (n.isEmpty()) {
         this.note = "§cвведи имя группы слева";
         return;
      }
      this.msg.createGroup(n);
      selected = "#" + n;
      this.scroll = 0;
      this.dmField.method_1852("");
      this.note = "§aгруппа «" + n + "» создана";
      this.rebuildKeepingDraft();
   }

   /** Rebuilds the screen (e.g. after a new tab appears) without losing a half-typed message. */
   private void rebuildKeepingDraft() {
      String draft = this.input != null ? this.input.method_1882() : "";
      this.method_41843();
      if (this.input != null) {
         this.input.method_1852(draft);
      }
   }

   private void doAddMember() {
      if (this.msg == null || selected == null || !selected.startsWith("#")) {
         return;
      }
      String n = this.dmField.method_1882().trim();
      if (n.startsWith("@")) {
         n = n.substring(1);
      }
      if (n.isEmpty()) {
         this.note = "§cвведи ник участника слева";
         return;
      }
      this.msg.addToGroup(n, selected.substring(1));
      this.dmField.method_1852("");
      this.note = "§a+" + n + " → " + selected;
   }

   private void doSend() {
      if (this.msg == null || selected == null) {
         return;
      }
      String text = this.input.method_1882();
      if (text.isBlank()) {
         return;
      }
      this.note = this.msg.send(selected, text);
      this.input.method_1852("");
      this.scroll = 0;
      this.method_41843();
   }

   public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
      int w = this.field_22789;
      int h = this.field_22790;
      ctx.method_25294(0, 0, w, h, BG);
      ctx.method_25294(0, 0, w, 30, HEADER);
      ctx.method_25294(0, 29, w, 31, ACCENT);
      ctx.method_51433(this.field_22793, "§5§lМессенджер", 12, 11, 0xFFD9C2FF, true);
      ctx.method_51433(this.field_22793, RelayClient.status(), w - 110, 11, 0xFFE7DAF6, true);

      ctx.method_25294(6, 36, LEFT_W + 4, h - 52, PANEL);
      ctx.method_25294(LEFT_W + 8, 36, w - 8, h - 30, PANEL);

      // Hint above the name box so the action buttons are self-explanatory.
      ctx.method_51433(this.field_22793, "§8ник→ЛС · имя→Группа", 10, h - 74, 0xFF8A7FA0, true);

      super.method_25394(ctx, mouseX, mouseY, delta);

      if (this.msg == null) {
         return;
      }
      if (selected == null) {
         ctx.method_51433(this.field_22793, "§7Выбери чат слева или создай ЛС.", LEFT_W + 16, 44, 0xFF9A8FB0, true);
         return;
      }

      // Header of the selected thread.
      ctx.method_51433(this.field_22793, "§d§l" + selected, LEFT_W + 16, 40, 0xFFD9C2FF, true);
      if (!this.note.isEmpty()) {
         ctx.method_51433(this.field_22793, this.note, LEFT_W + 120, 40, 0xFFE7DAF6, true);
      }

      // Messages, Discord style: avatar + name/time header, then wrapped lines.
      int areaX = LEFT_W + 16;
      int areaW = w - areaX - 16;
      int top = 54;
      int bottom = this.field_22790 - 34;

      List<Messenger.Msg> list = this.msg.messages(selected);
      List<Object[]> blocks = new ArrayList<>(); // {type("h"/"t"), text, from}
      String prevFrom = null;
      for (Messenger.Msg m : list) {
         if (!m.from.equals(prevFrom)) {
            String when = TIME.format(Instant.ofEpochMilli(m.ts).atZone(ZoneId.systemDefault()));
            blocks.add(new Object[]{"h", (m.out ? "§a" : "§b") + m.from + " §8" + when, m.from});
            prevFrom = m.from;
         }
         for (String lineText : this.wrap(m.text, areaW - 24)) {
            blocks.add(new Object[]{"t", "§r" + lineText, m.from});
         }
      }

      // Fit from the bottom: header rows are taller (avatar).
      int[] heights = new int[blocks.size()];
      int total = 0;
      for (int i = 0; i < blocks.size(); ++i) {
         heights[i] = blocks.get(i)[0].equals("h") ? 14 : 10;
      }
      if (this.scroll > blocks.size() - 1) {
         this.scroll = Math.max(0, blocks.size() - 1);
      }
      int end = Math.max(1, blocks.size() - this.scroll);
      int start = end;
      int used = 0;
      while (start > 0 && used + heights[start - 1] <= bottom - top) {
         --start;
         used += heights[start];
      }
      if (this.scroll > 0) {
         ctx.method_51433(this.field_22793, "§8▼ к новым — колесо вниз", areaX, bottom + 1, 0xFF8A7FA0, true);
      }
      int yy = top;
      for (int i = start; i < end; ++i) {
         Object[] b = blocks.get(i);
         if (b[0].equals("h")) {
            yy += 2;
            this.drawAvatar(ctx, (String) b[2], areaX, yy);
            ctx.method_51433(this.field_22793, (String) b[1], areaX + 16, yy + 2, 0xFFE7DAF6, true);
            yy += 12;
         } else {
            ctx.method_51433(this.field_22793, (String) b[1], areaX + 16, yy, 0xFFE7DAF6, true);
            yy += 10;
         }
      }
   }

   /** Player-head avatar from the tab list; a lettered square when offline. */
   private void drawAvatar(class_332 ctx, String name, int x, int y) {
      class_310 mc = class_310.method_1551();
      if (mc.method_1562() != null) {
         for (class_640 entry : mc.method_1562().method_2880()) {
            if (entry.method_2966() != null && entry.method_2966().name().equalsIgnoreCase(name)) {
               class_7532.method_44443(ctx, entry.method_52810(), x, y, 12, -1);
               return;
            }
         }
      }
      ctx.method_25294(x, y, x + 12, y + 12, 0xFF3A2B55);
      String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
      ctx.method_51433(this.field_22793, letter, x + 3, y + 2, 0xFFD9C2FF, true);
   }

   private List<String> wrap(String text, int width) {
      List<String> out = new ArrayList<>();
      StringBuilder cur = new StringBuilder();
      for (String word : text.split(" ")) {
         String probe = cur.isEmpty() ? word : cur + " " + word;
         if (this.field_22793.method_1727(probe) > width && !cur.isEmpty()) {
            out.add(cur.toString());
            cur = new StringBuilder(word);
         } else {
            cur = new StringBuilder(probe);
         }
      }
      if (!cur.isEmpty()) {
         out.add(cur.toString());
      }
      return out;
   }

   public boolean method_25421() {
      return false;
   }
}
