package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Tracks your server-economy activity by reading the game chat: purchases,
 * transfers you send / receive, sales, and balance snapshots. Every event is
 * logged to disk so the {@code /kafka money} screen can show spending over the
 * day / week / month. Patterns are tuned for vanilla-box.ru (Базар / «отправлено
 * игроку» / «осталось N ◎») but tolerate spaces and comma thousands separators.
 */
public class EconomyTracker extends Module {
   public enum Kind { PURCHASE, TRANSFER_OUT, TRANSFER_IN, SALE }

   private static final Path LOG = FabricLoader.getInstance().getConfigDir().resolve("kafkautils-economy.log");
   private static final int LOAD_LIMIT = 5000;
   private static boolean openRequested;

   // Amount groups allow digits, spaces and commas (thousands separators).
   private static final Pattern P_BALANCE = Pattern.compile("(?:осталось|баланс)\\s*[:»]?\\s*([\\d.,\\s]+?)\\s*◎", Pattern.CASE_INSENSITIVE);
   private static final Pattern P_OUT = Pattern.compile("^\\s*([\\d.,\\s]+?)\\s*◎?\\s*отправлено\\s+игроку\\s+([^\\s]+?)\\.?\\s*$", Pattern.CASE_INSENSITIVE);
   private static final Pattern P_BUY = Pattern.compile("вы\\s+купили\\s+(.+?)\\s+за\\s+([\\d.,\\s]+?)\\s*◎", Pattern.CASE_INSENSITIVE);
   private static final Pattern P_SELLER = Pattern.compile("у\\s+\\[([^\\]]+)\\]");
   private static final Pattern P_SALE = Pattern.compile("вы\\s+продали\\s+(.+?)\\s+за\\s+([\\d.,\\s]+?)\\s*◎", Pattern.CASE_INSENSITIVE);
   // Incoming transfer — format unconfirmed; two common phrasings, refined later.
   private static final Pattern P_IN = Pattern.compile("(?:игрок\\s+([^\\s]+)\\s+отправил\\s+вам\\s+([\\d.,\\s]+)|([\\d.,\\s]+?)\\s*◎?\\s*получено\\s+от\\s+игрока\\s+([^\\s]+))", Pattern.CASE_INSENSITIVE);

   private final StringSetting currency = this.add(new StringSetting("Currency", "◎"));
   private final BooleanSetting notify = this.add(new BooleanSetting("Chat Notify", false));

   private final List<Txn> txns = new ArrayList<>();
   private long balance;
   private boolean balanceKnown;

   public EconomyTracker() {
      super("Economy Tracker", "Учёт баланса: покупки, переводы, траты за день/неделю/месяц (/kafka money).", Category.CHAT);
   }

   public static void requestOpen() {
      openRequested = true;
   }

   public static boolean consumeOpen() {
      boolean r = openRequested;
      openRequested = false;
      return r;
   }

   protected void onEnable() {
      this.load();
   }

   /** Called for every incoming game message. Observes only (never hides it). */
   public void handleMessage(String raw) {
      if (raw == null) {
         return;
      }
      String text = raw.replaceAll("§.", "").trim();

      Matcher mb = P_BALANCE.matcher(text);
      if (mb.find()) {
         long b = parseAmount(mb.group(1));
         if (b >= 0) {
            this.balance = b;
            this.balanceKnown = true;
         }
         return;
      }
      Matcher mo = P_OUT.matcher(text);
      if (mo.find()) {
         this.record(Kind.TRANSFER_OUT, parseAmount(mo.group(1)), mo.group(2), "");
         return;
      }
      Matcher mbuy = P_BUY.matcher(text);
      if (mbuy.find()) {
         String seller = "";
         Matcher ms = P_SELLER.matcher(text);
         if (ms.find()) {
            seller = ms.group(1);
         }
         this.record(Kind.PURCHASE, parseAmount(mbuy.group(2)), seller, mbuy.group(1).trim());
         return;
      }
      Matcher msale = P_SALE.matcher(text);
      if (msale.find()) {
         this.record(Kind.SALE, parseAmount(msale.group(2)), "", msale.group(1).trim());
         return;
      }
      Matcher min = P_IN.matcher(text);
      if (min.find()) {
         String who = min.group(1) != null ? min.group(1) : min.group(4);
         String amt = min.group(2) != null ? min.group(2) : min.group(3);
         this.record(Kind.TRANSFER_IN, parseAmount(amt), who, "");
      }
   }

   private void record(Kind kind, long amount, String who, String note) {
      if (amount <= 0) {
         return;
      }
      Txn t = new Txn(System.currentTimeMillis(), kind, amount, who == null ? "" : who, note == null ? "" : note);
      this.txns.add(t);
      this.persist(t);
      if (this.balanceKnown) {
         this.balance += t.income() ? amount : -amount;
      }
      if (this.notify.get()) {
         ChatUtil.info((t.income() ? "§a+" : "§c-") + fmt(amount) + " " + this.currency.get() + " §7"
            + label(kind) + (who.isEmpty() ? "" : " §f" + who));
      }
   }

   // --- accessors for the GUI / HUD ---------------------------------------

   public List<Txn> transactions() {
      return this.txns;
   }

   public String currency() {
      return this.currency.get();
   }

   public long balance() {
      return this.balance;
   }

   public boolean balanceKnown() {
      return this.balanceKnown;
   }

   public long spentSince(long since) {
      long s = 0;
      for (Txn t : this.txns) {
         if (t.ts >= since && !t.income()) {
            s += t.amount;
         }
      }
      return s;
   }

   public long incomeSince(long since) {
      long s = 0;
      for (Txn t : this.txns) {
         if (t.ts >= since && t.income()) {
            s += t.amount;
         }
      }
      return s;
   }

   public long spentToday() {
      return this.spentSince(startOfToday());
   }

   public long spentWeek() {
      return this.spentSince(startOfWeek());
   }

   public long spentMonth() {
      return this.spentSince(startOfMonth());
   }

   public long incomeToday() {
      return this.incomeSince(startOfToday());
   }

   public long incomeWeek() {
      return this.incomeSince(startOfWeek());
   }

   public long incomeMonth() {
      return this.incomeSince(startOfMonth());
   }

   /** Prints a short summary to chat (used by /kafka money today). */
   public void printSummary() {
      ChatUtil.info("§d§lЭкономика §7— баланс: §f" + (this.balanceKnown ? fmt(this.balance) : "?") + " " + this.currency.get());
      ChatUtil.info("§7Сегодня: §c-" + fmt(this.spentToday()) + " §7/ §a+" + fmt(this.incomeToday()) + " " + this.currency.get());
      ChatUtil.info("§7Неделя: §c-" + fmt(this.spentWeek()) + " §7/ §a+" + fmt(this.incomeWeek()) + " " + this.currency.get());
      ChatUtil.info("§7Месяц: §c-" + fmt(this.spentMonth()) + " §7/ §a+" + fmt(this.incomeMonth()) + " " + this.currency.get());
   }

   public void reset() {
      this.txns.clear();
      try {
         Files.deleteIfExists(LOG);
      } catch (Exception ignored) {
      }
      ChatUtil.info("§d[Экономика] §7история очищена.");
   }

   public static String fmt(long v) {
      return String.format(Locale.ROOT, "%,d", v).replace(',', ' ');
   }

   public static String label(Kind k) {
      return switch (k) {
         case PURCHASE -> "покупка";
         case TRANSFER_OUT -> "перевод";
         case TRANSFER_IN -> "перевод вам";
         case SALE -> "продажа";
      };
   }

   // --- helpers -----------------------------------------------------------

   private static long parseAmount(String s) {
      if (s == null) {
         return -1;
      }
      StringBuilder d = new StringBuilder();
      for (int i = 0; i < s.length(); ++i) {
         char c = s.charAt(i);
         if (c >= '0' && c <= '9') {
            d.append(c);
         }
      }
      if (d.length() == 0) {
         return -1;
      }
      try {
         return Long.parseLong(d.toString());
      } catch (Exception e) {
         return -1;
      }
   }

   private static long startOfToday() {
      return LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
   }

   private static long startOfWeek() {
      LocalDate d = LocalDate.now(ZoneId.systemDefault());
      d = d.minusDays((d.getDayOfWeek().getValue() + 6) % 7); // Monday as the first day
      return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
   }

   private static long startOfMonth() {
      return LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1)
         .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
   }

   private void persist(Txn t) {
      try {
         String line = t.ts + "|" + t.kind.name() + "|" + t.amount + "|" + enc(t.who) + "|" + enc(t.note) + System.lineSeparator();
         Files.writeString(LOG, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (Exception ignored) {
      }
   }

   private void load() {
      this.txns.clear();
      try {
         if (!Files.exists(LOG)) {
            return;
         }
         List<String> lines = Files.readAllLines(LOG, StandardCharsets.UTF_8);
         int from = Math.max(0, lines.size() - LOAD_LIMIT);
         for (int i = from; i < lines.size(); ++i) {
            String[] p = lines.get(i).split("\\|", -1);
            if (p.length >= 5) {
               try {
                  this.txns.add(new Txn(Long.parseLong(p[0]), Kind.valueOf(p[1]), Long.parseLong(p[2]), dec(p[3]), dec(p[4])));
               } catch (Exception ignored) {
               }
            }
         }
      } catch (Exception ignored) {
      }
   }

   private static String enc(String s) {
      return s.replace("\\", "\\\\").replace("|", "/").replace("\n", " ");
   }

   private static String dec(String s) {
      return s.replace("\\\\", "\\");
   }

   /** One economy event: time, kind, amount (positive), counterparty and a note. */
   public static final class Txn {
      public final long ts;
      public final Kind kind;
      public final long amount;
      public final String who;
      public final String note;

      public Txn(long ts, Kind kind, long amount, String who, String note) {
         this.ts = ts;
         this.kind = kind;
         this.amount = amount;
         this.who = who;
         this.note = note;
      }

      public boolean income() {
         return this.kind == Kind.TRANSFER_IN || this.kind == Kind.SALE;
      }
   }
}
