package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import dev.kafka.kafkautils.util.RenderUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2558;
import net.minecraft.class_2561;
import net.minecraft.class_2568;
import net.minecraft.class_332;
import net.minecraft.class_5250;

/**
 * PocketChat-style private-message upgrade (chat side): parses whispers with
 * configurable regexes (so any server format works), optionally hides the raw
 * line and renders a clean clickable bubble line instead, keeps a permanent
 * local history with full-text search, and shows unread counters in the HUD.
 *
 * <p>Regexes need two capture groups: (1) player name, (2) message text.
 */
public class PrivateMessages extends Module implements HudModule {
   private static final Path LOG = FabricLoader.getInstance().getConfigDir().resolve("kafkautils-pm.log");
   private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm");
   private static final int MAX_RESULTS = 15;

   private final StringSetting incoming = this.add(new StringSetting("Incoming Regex",
      "\\(ЛС\\)\\s*(\\S{3,16})\\s*->\\s*я\\s*»?\\s*(.+)"));
   private final StringSetting outgoing = this.add(new StringSetting("Outgoing Regex",
      "\\(ЛС\\)\\s*я\\s*->\\s*(\\S{3,16})\\s*»?\\s*(.+)"));
   private final BooleanSetting pretty = this.add(new BooleanSetting("Pretty Render", true));
   private final BooleanSetting hudUnread = this.add(new BooleanSetting("Unread HUD", true));

   private final Map<String, Integer> unread = new LinkedHashMap<>();
   private final Map<String, String> displayNames = new LinkedHashMap<>();

   public PrivateMessages() {
      super("Private Messages", "Личка: красивый вид, история с поиском (/kafka pm), непрочитанные в HUD.", Category.CHAT);
   }

   protected void onDisable() {
      this.unread.clear();
   }

   /** Incoming server line. @return true to hide the raw whisper (pretty mode). */
   public boolean handleIncoming(String raw) {
      if (!this.isEnabled() || raw == null) {
         return false;
      }
      String plain = raw.replaceAll("§.", "");

      String[] out = match(this.outgoing.get(), plain);
      if (out != null) {
         this.record("out", out[0], out[1]);
         this.unread.remove(out[0].toLowerCase(Locale.ROOT));
         if (this.pretty.get()) {
            this.bubble("§d✉ §7ты → §b" + out[0] + "§7: §r" + out[1], out[0]);
            return true;
         }
         return false;
      }

      String[] in = match(this.incoming.get(), plain);
      if (in != null) {
         this.record("in", in[0], in[1]);
         this.unread.merge(in[0].toLowerCase(Locale.ROOT), 1, Integer::sum);
         this.displayNames.put(in[0].toLowerCase(Locale.ROOT), in[0]);
         if (this.pretty.get()) {
            this.bubble("§d✉ §b" + in[0] + " §7→ тебе: §r" + in[1], in[0]);
            return true;
         }
      }
      return false;
   }

   /** Outgoing command hook: sending a whisper marks that thread as read. */
   public void onCommand(String command) {
      if (!this.isEnabled() || command == null) {
         return;
      }
      String[] parts = command.trim().split("\\s+", 3);
      if (parts.length >= 3) {
         String c = parts[0].toLowerCase(Locale.ROOT);
         if (c.equals("msg") || c.equals("m") || c.equals("w") || c.equals("tell") || c.equals("whisper") || c.equals("pm") || c.equals("message")) {
            this.unread.remove(parts[1].toLowerCase(Locale.ROOT));
         }
      }
   }

   /** Clickable pretty line: click fills /msg <name> for a quick reply. */
   private void bubble(String line, String name) {
      class_5250 text = class_2561.method_43470(line);
      text.method_10862(text.method_10866()
         .method_10958(new class_2558.class_10610("/msg " + name + " "))
         .method_10949(new class_2568.class_10613(class_2561.method_43470("§7Ответить §r" + name))));
      ChatUtil.text(text);
   }

   public void clearUnread() {
      this.unread.clear();
   }

   // --- history -----------------------------------------------------------

   private void record(String dir, String name, String text) {
      try {
         String line = System.currentTimeMillis() + "\t" + dir + "\t" + name + "\t"
            + text.replace('\t', ' ').replace('\n', ' ') + System.lineSeparator();
         Files.writeString(LOG, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (Exception ignored) {
      }
   }

   public void printSearch(String query) {
      List<String[]> rows = this.readLog();
      List<String[]> hits = new ArrayList<>();
      String q = query.toLowerCase(Locale.ROOT);
      for (String[] r : rows) {
         if (r[3].toLowerCase(Locale.ROOT).contains(q) || r[2].toLowerCase(Locale.ROOT).contains(q)) {
            hits.add(r);
         }
      }
      this.printRows("Поиск: " + query, hits);
   }

   public void printHistory(String name) {
      List<String[]> rows = this.readLog();
      List<String[]> hits = new ArrayList<>();
      for (String[] r : rows) {
         if (r[2].equalsIgnoreCase(name)) {
            hits.add(r);
         }
      }
      this.printRows("История: " + name, hits);
   }

   private void printRows(String title, List<String[]> hits) {
      ChatUtil.info("§d§l— " + title + " §r§7(" + hits.size() + ")");
      if (hits.isEmpty()) {
         ChatUtil.info("§7Ничего не найдено.");
         return;
      }
      int from = Math.max(0, hits.size() - MAX_RESULTS);
      if (from > 0) {
         ChatUtil.info("§8… показаны последние " + MAX_RESULTS);
      }
      for (int i = from; i < hits.size(); ++i) {
         String[] r = hits.get(i);
         String when = TIME.format(Instant.ofEpochMilli(Long.parseLong(r[0])).atZone(ZoneId.systemDefault()));
         String arrow = r[1].equals("out") ? "§7ты → §b" + r[2] : "§b" + r[2] + " §7→ тебе";
         ChatUtil.raw("§8[" + when + "] §r" + arrow + "§7: §r" + r[3]);
      }
   }

   private List<String[]> readLog() {
      List<String[]> rows = new ArrayList<>();
      try {
         if (Files.exists(LOG)) {
            for (String line : Files.readAllLines(LOG, StandardCharsets.UTF_8)) {
               String[] p = line.split("\t", 4);
               if (p.length == 4) {
                  rows.add(p);
               }
            }
         }
      } catch (Exception ignored) {
      }
      return rows;
   }

   // --- helpers -----------------------------------------------------------

   /** @return {name, text} when the regex (2 groups) finds a match, else null. */
   private static String[] match(String regex, String plain) {
      try {
         Matcher m = Pattern.compile(regex).matcher(plain);
         if (m.find() && m.groupCount() >= 2) {
            String name = m.group(1);
            String text = m.group(2);
            if (name != null && text != null && !name.isBlank() && !text.isBlank()) {
               return new String[]{name.trim(), text.trim()};
            }
         }
      } catch (Exception ignored) {
      }
      return null;
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      if (!this.hudUnread.get() || this.unread.isEmpty()) {
         return new int[]{0, 0};
      }
      List<String> lines = new ArrayList<>();
      for (Map.Entry<String, Integer> e : this.unread.entrySet()) {
         String name = this.displayNames.getOrDefault(e.getKey(), e.getKey());
         lines.add("§b" + name + "§7: §d" + e.getValue());
      }
      return RenderUtil.panel(ctx, x, y, "✉ Непрочитанные", lines);
   }
}
