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
import net.minecraft.class_11909;
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
 * unread badges and online dots), messages on the right with head avatars, plus
 * search, typing indicators, group member list / leave, and edit/delete of your
 * own messages (click a message to act on it). Opened with /kafka pm.
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
   private class_342 searchField;
   private String note = "";
   private int scroll;
   private boolean showMembers;
   private Messenger.Msg actionMsg;   // your own message clicked, awaiting edit/delete
   private String editingId;          // id being edited, or null
   private String editDraft = "";     // text to prefill when entering edit mode
   private final List<Object[]> rowHits = new ArrayList<>(); // {int yTop, int yBottom, Msg}

   public MessengerScreen() {
      super(class_2561.method_43470("Мессенджер"));
      this.msg = ModuleManager.get(Messenger.class);
   }

   private class_4185 btn(String label, int x, int y, int w, int h, Runnable r) {
      return class_4185.method_46430(class_2561.method_43470(label), (b) -> r.run()).method_46434(x, y, w, h).method_46431();
   }

   protected void method_25426() {
      if (this.msg == null) {
         return;
      }
      int w = this.field_22789;
      int h = this.field_22790;

      // Left top: search across all chats.
      this.searchField = new class_342(this.field_22793, 10, 38, LEFT_W - 6, 13, class_2561.method_43470("поиск"));
      this.searchField.method_1880(48);
      this.searchField.method_47404(class_2561.method_43470("§7поиск по чатам…"));
      this.method_37063(this.searchField);

      // Left: thread tabs (with online dot for DMs).
      int y = 56;
      for (String key : this.msg.threadKeys()) {
         final String k = key;
         int un = this.msg.unreadOf(key);
         String dot = k.startsWith("@") ? (this.msg.isOnline(k.substring(1)) ? "§a● " : "§7○ ") : "§d# ";
         String name = k.startsWith("#") ? k.substring(1) : k;
         String label = dot + (k.equals(selected) ? "§f§l" : "§7") + name + (un > 0 ? " §c(" + un + ")" : "");
         this.method_37063(this.btn(label, 10, y, LEFT_W - 6, 14, () -> {
            selected = k;
            this.scroll = 0;
            this.actionMsg = null;
            this.editingId = null;
            this.showMembers = false;
            this.msg.openThread(k);
            this.method_41843();
         }));
         y += 16;
      }

      // Left bottom: name field + actions.
      int nameY = h - 62;
      this.dmField = new class_342(this.field_22793, 10, nameY, LEFT_W - 6, 14, class_2561.method_43470("ник / группа"));
      this.dmField.method_1880(24);
      this.dmField.method_47404(class_2561.method_43470("§7ник / группа…"));
      this.method_37063(this.dmField);

      int actY = h - 44;
      this.method_37063(this.btn("§aЛС", 10, actY, 44, 14, this::doOpenDm));
      this.method_37063(this.btn("§bГруппа", 58, actY, 54, 14, this::doCreateGroup));
      if (selected != null && selected.startsWith("#")) {
         this.method_37063(this.btn("§d+уч", 116, actY, 34, 14, this::doAddMember));
      }

      // Right header: group tools (member list toggle + leave).
      if (selected != null && selected.startsWith("#")) {
         this.method_37063(this.btn("§bУчастники", w - 152, 37, 80, 13, () -> {
            this.showMembers = !this.showMembers;
            if (this.showMembers) {
               this.msg.requestMembers(selected.substring(1));
            }
         }));
         this.method_37063(this.btn("§cВыйти", w - 68, 37, 58, 13, () -> {
            this.msg.leaveGroup(selected.substring(1));
            selected = null;
            this.showMembers = false;
            this.method_41843();
         }));
      }

      // Right header second row: edit/delete for a clicked own message.
      if (this.actionMsg != null && this.actionMsg.out && !this.actionMsg.deleted && selected != null) {
         this.method_37063(this.btn("§e✎ Ред.", LEFT_W + 16, 51, 58, 13, this::beginEdit));
         this.method_37063(this.btn("§c✕ Удал.", LEFT_W + 78, 51, 58, 13, this::doDelete));
         this.method_37063(this.btn("§7Отмена", LEFT_W + 140, 51, 58, 13, () -> {
            this.actionMsg = null;
            this.method_41843();
         }));
      } else if (this.editingId != null && selected != null) {
         this.method_37063(this.btn("§7✖ отменить редактирование", LEFT_W + 16, 51, 170, 13, () -> {
            this.editingId = null;
            this.editDraft = "";
            this.method_41843();
         }));
      }

      // Bottom: message input + send (or save when editing).
      int inY = h - 24;
      int sendW = this.editingId != null ? 96 : 72;
      this.input = new class_342(this.field_22793, LEFT_W + 12, inY, w - LEFT_W - sendW - 28, 16, class_2561.method_43470("Сообщение"));
      this.input.method_1880(256);
      this.input.method_47404(class_2561.method_43470(this.editingId != null ? "§eредактируешь…" : "§7Сообщение…"));
      if (this.editingId != null) {
         this.input.method_1852(this.editDraft);
      }
      this.method_37063(this.input);
      this.method_37063(this.btn(this.editingId != null ? "§eСохранить" : "§aОтправить", w - sendW - 10, inY, sendW, 16, this::doSend));

      this.method_48265(this.input);
   }

   /** Enter sends/saves; any other key while typing signals "typing…". */
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
      boolean handled = super.method_25404(key);
      if (this.msg != null && this.input != null && this.input.method_25370() && selected != null && this.editingId == null) {
         this.msg.sendTyping(selected);
      }
      return handled;
   }

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

   /** Click one of your own messages to bring up edit/delete. */
   public boolean method_25402(class_11909 click, boolean doubled) {
      if (click.method_74245() == 0 && selected != null && (this.searchField == null || this.searchField.method_1882().isBlank())) {
         int mx = (int) click.comp_4798();
         int my = (int) click.comp_4799();
         if (mx > LEFT_W + 8) {
            for (Object[] hit : this.rowHits) {
               if (my >= (int) hit[0] && my < (int) hit[1]) {
                  Messenger.Msg m = (Messenger.Msg) hit[2];
                  if (m.out && !m.deleted) {
                     this.actionMsg = m;
                     this.method_41843();
                     return true;
                  }
               }
            }
         }
      }
      return super.method_25402(click, doubled);
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
      this.actionMsg = null;
      this.editingId = null;
      this.msg.messages(selected);
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

   private void beginEdit() {
      if (this.actionMsg == null) {
         return;
      }
      this.editingId = this.actionMsg.id;
      this.editDraft = this.actionMsg.text;
      this.actionMsg = null;
      this.method_41843();
   }

   private void doDelete() {
      if (this.actionMsg == null || selected == null) {
         return;
      }
      this.msg.deleteMessage(selected, this.actionMsg.id);
      this.actionMsg = null;
      this.method_41843();
   }

   private void rebuildKeepingDraft() {
      String draft = this.input != null ? this.input.method_1882() : "";
      this.method_41843();
      if (this.input != null && this.editingId == null) {
         this.input.method_1852(draft);
      }
   }

   private void doSend() {
      if (this.msg == null || selected == null) {
         return;
      }
      String text = this.input.method_1882();
      if (text.isBlank()) {
         return;
      }
      if (this.editingId != null) {
         this.msg.editMessage(selected, this.editingId, text);
         this.editingId = null;
         this.editDraft = "";
         this.note = "§7изменено";
      } else {
         this.note = this.msg.send(selected, text);
      }
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

      ctx.method_25294(6, 36, LEFT_W + 4, h - 66, PANEL);
      ctx.method_25294(LEFT_W + 8, 36, w - 8, h - 30, PANEL);

      super.method_25394(ctx, mouseX, mouseY, delta);
      this.rowHits.clear();

      if (this.msg == null) {
         return;
      }

      // Search mode replaces the message area with global results.
      if (this.searchField != null && !this.searchField.method_1882().isBlank()) {
         List<String> res = this.msg.search(this.searchField.method_1882().trim());
         ctx.method_51433(this.field_22793, "§d§lПоиск §7(" + res.size() + ")", LEFT_W + 16, 40, 0xFFD9C2FF, true);
         int ry = 56;
         for (String line : res) {
            if (ry > h - 20) {
               break;
            }
            ctx.method_51433(this.field_22793, line, LEFT_W + 16, ry, 0xFFE7DAF6, true);
            ry += 10;
         }
         if (res.isEmpty()) {
            ctx.method_51433(this.field_22793, "§7ничего не найдено", LEFT_W + 16, 56, 0xFF9A8FB0, true);
         }
         return;
      }

      if (selected == null) {
         ctx.method_51433(this.field_22793, "§7Выбери чат слева или создай ЛС.", LEFT_W + 16, 44, 0xFF9A8FB0, true);
         return;
      }

      // Thread header with online state for DMs.
      String title = "§d§l" + selected;
      if (selected.startsWith("@")) {
         title += this.msg.isOnline(selected.substring(1)) ? " §a●" : " §7○";
      }
      ctx.method_51433(this.field_22793, title, LEFT_W + 16, 40, 0xFFD9C2FF, true);
      if (!this.note.isEmpty() && this.actionMsg == null) {
         ctx.method_51433(this.field_22793, this.note, LEFT_W + 120, 40, 0xFFE7DAF6, true);
      }

      int areaX = LEFT_W + 16;
      int areaW = w - areaX - 16;
      int top = (this.actionMsg != null || this.editingId != null) ? 66 : 54;
      int bottom = h - 40;

      List<Messenger.Msg> list = this.msg.messages(selected);
      List<Object[]> blocks = new ArrayList<>(); // {type, text, Msg}
      String prevFrom = null;
      for (Messenger.Msg m : list) {
         if (!m.from.equals(prevFrom)) {
            String when = TIME.format(Instant.ofEpochMilli(m.ts).atZone(ZoneId.systemDefault()));
            blocks.add(new Object[]{"h", (m.out ? "§a" : "§b") + m.from + " §8" + when, m});
            prevFrom = m.from;
         }
         if (m.deleted) {
            blocks.add(new Object[]{"t", "§8[удалено]", m});
         } else {
            List<String> lines = this.wrap(m.text, areaW - 24);
            for (int li = 0; li < lines.size(); ++li) {
               String t = "§r" + lines.get(li);
               if (m.edited && li == lines.size() - 1) {
                  t += " §8(ред.)";
               }
               blocks.add(new Object[]{"t", t, m});
            }
         }
      }

      int[] heights = new int[blocks.size()];
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
      int yy = top;
      for (int i = start; i < end; ++i) {
         Object[] b = blocks.get(i);
         Messenger.Msg m = (Messenger.Msg) b[2];
         if (b[0].equals("h")) {
            yy += 2;
            this.drawAvatar(ctx, m.from, areaX, yy);
            ctx.method_51433(this.field_22793, (String) b[1], areaX + 16, yy + 2, 0xFFE7DAF6, true);
            this.rowHits.add(new Object[]{yy, yy + 12, m});
            yy += 12;
         } else {
            int col = m == this.actionMsg ? 0xFFFFE08A : 0xFFE7DAF6;
            ctx.method_51433(this.field_22793, (String) b[1], areaX + 16, yy, col, true);
            this.rowHits.add(new Object[]{yy, yy + 10, m});
            yy += 10;
         }
      }

      if (this.scroll > 0) {
         ctx.method_51433(this.field_22793, "§8▲ история — колесо вниз к новым", areaX, top - 12, 0xFF8A7FA0, true);
      }

      // Typing indicator / hint just above the input.
      String typing = this.msg.typingText(selected);
      if (typing != null) {
         ctx.method_51433(this.field_22793, "§7" + typing, areaX, h - 37, 0xFF9A8FB0, true);
      } else if (this.actionMsg == null) {
         ctx.method_51433(this.field_22793, "§8клик по своему сообщению — ред./удал.", areaX, h - 37, 0xFF6A6080, true);
      }

      // Optional group member overlay.
      if (this.showMembers && selected.startsWith("#")) {
         List<String> members = this.msg.membersOf(selected.substring(1));
         int mw = 130;
         int mx = w - mw - 12;
         ctx.method_25294(mx, 54, mx + mw, 54 + 12 + members.size() * 10 + 6, 0xEE1B1030);
         ctx.method_51433(this.field_22793, "§d§lУчастники", mx + 6, 58, 0xFFD9C2FF, true);
         int my = 70;
         for (String name : members) {
            String d = this.msg.isOnline(name) ? "§a● " : "§7○ ";
            ctx.method_51433(this.field_22793, d + "§r" + name, mx + 6, my, 0xFFE7DAF6, true);
            my += 10;
         }
         if (members.isEmpty()) {
            ctx.method_51433(this.field_22793, "§7…", mx + 6, my, 0xFF9A8FB0, true);
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
