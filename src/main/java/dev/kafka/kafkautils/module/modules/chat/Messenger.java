package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import dev.kafka.kafkautils.util.RelayClient;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Discord-style messenger between Kafka-Utils users. Primary transport is the
 * relay server ({@link RelayClient}) — messages never touch the Minecraft
 * server. When the relay is unreachable, direct messages fall back to hidden
 * marker whispers through Friend Chat (the game server sees those). Threads:
 * {@code @nick} for DMs and {@code #group} for relay-side groups.
 */
public class Messenger extends Module {
   /**
    * Prefix routing a fallback whisper payload into the messenger window.
    * Printable ASCII on purpose: it is sent through the server's whisper
    * command, which rejects control characters and kicks the sender.
    */
   public static final String FALLBACK = "~kfm~";

   private static final Path LOG = FabricLoader.getInstance().getConfigDir().resolve("kafkautils-messenger.log");
   private static final int LOAD_LIMIT = 300;
   private static boolean openRequested;

   private final StringSetting url = this.add(new StringSetting("Server URL", "ws://163.123.180.81:10041"));
   private final StringSetting key = this.add(new StringSetting("Key", "changeme"));
   private final BooleanSetting fallback = this.add(new BooleanSetting("Fallback via /msg", true));
   private final BooleanSetting notify = this.add(new BooleanSetting("Chat Notify", true));

   private final Map<String, List<Msg>> threads = new LinkedHashMap<>();
   private final Map<String, Integer> unread = new LinkedHashMap<>();
   private final Set<String> online = new LinkedHashSet<>();
   private final Set<String> groups = new LinkedHashSet<>();
   private boolean announcedUp;

   public Messenger() {
      super("Messenger", "Мессенджер между владельцами мода через свой релей (/kafka pm).", Category.FRIENDS);
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
      this.loadHistory();
   }

   protected void onDisable() {
      RelayClient.close();
      this.announcedUp = false;
   }

   public void onTick() {
      if (mc.field_1724 == null) {
         return;
      }
      RelayClient.ensure(this.url.get(), this.key.get(), this.myName());
      String line;
      while ((line = RelayClient.INBOX.poll()) != null) {
         this.handle(line);
      }
   }

   // --- inbound -----------------------------------------------------------

   private void handle(String line) {
      String[] p = line.split("\\|", -1);
      switch (p[0]) {
         case "ok" -> {
            if (!this.announcedUp) {
               this.announcedUp = true;
               ChatUtil.info("§a[Мессенджер] §7подключено к релею.");
            }
         }
         case "err" -> {
            if (p.length >= 2) {
               ChatUtil.info("§d[Мессенджер] §7" + dec(p[1]));
            }
         }
         case "msg" -> {
            if (p.length >= 3) {
               String from = dec(p[1]);
               this.incoming("@" + from, from, dec(p[2]));
            }
         }
         case "gmsg" -> {
            if (p.length >= 4) {
               String group = dec(p[1]);
               this.groups.add(group);
               this.incoming("#" + group, dec(p[2]), dec(p[3]));
            }
         }
         case "users" -> {
            this.online.clear();
            if (p.length >= 2) {
               for (String n : dec(p[1]).split(",")) {
                  if (!n.isBlank()) {
                     this.online.add(n);
                  }
               }
            }
         }
         case "join" -> {
            if (p.length >= 2) {
               this.online.add(dec(p[1]));
            }
         }
         case "left" -> {
            if (p.length >= 2) {
               this.online.remove(dec(p[1]));
            }
         }
         case "groups" -> {
            this.groups.clear();
            if (p.length >= 2) {
               for (String g : dec(p[1]).split(",")) {
                  if (!g.isBlank()) {
                     this.groups.add(g);
                     this.thread("#" + g); // make the tab visible
                  }
               }
            }
         }
         default -> { }
      }
   }

   /** A fallback whisper (via Friend Chat) routed into the messenger window. */
   public void onFallback(String sender, String text) {
      this.incoming("@" + sender, sender, text);
   }

   private void incoming(String threadKey, String from, String text) {
      Msg m = new Msg(from, text, System.currentTimeMillis(), false);
      this.thread(threadKey).add(m);
      this.persist(threadKey, m);
      this.unread.merge(threadKey, 1, Integer::sum);
      if (this.notify.get()) {
         ChatUtil.raw("§d✉ §b" + from + (threadKey.startsWith("#") ? " §7(" + threadKey + ")" : "") + "§7: §r" + text);
      }
   }

   // --- outbound ----------------------------------------------------------

   /** Sends into a thread; returns a short status note for the GUI. */
   public String send(String threadKey, String text) {
      text = text.trim();
      if (text.isEmpty()) {
         return "";
      }
      Msg m = new Msg(this.myName(), text, System.currentTimeMillis(), true);
      this.thread(threadKey).add(m);
      this.persist(threadKey, m);

      if (RelayClient.isAuthed()) {
         if (threadKey.startsWith("@")) {
            RelayClient.send("msg|" + RelayClient.enc(threadKey.substring(1)) + "|" + RelayClient.enc(text));
         } else {
            RelayClient.send("gmsg|" + RelayClient.enc(threadKey.substring(1)) + "|" + RelayClient.enc(text));
         }
         return "";
      }
      if (this.fallback.get() && threadKey.startsWith("@")) {
         FriendChat fc = ModuleManager.get(FriendChat.class);
         if (fc != null && fc.isEnabled()) {
            fc.whisper(threadKey.substring(1), FALLBACK + text);
            return "§eчерез /msg (релей оффлайн)";
         }
      }
      return "§cрелей недоступен" + (threadKey.startsWith("#") ? " — группы только через релей" : "");
   }

   public void createGroup(String name) {
      if (!RelayClient.isAuthed()) {
         ChatUtil.info("§d[Мессенджер] §cрелей недоступен — группы требуют подключения.");
         return;
      }
      RelayClient.send("gcreate|" + RelayClient.enc(name));
   }

   public void addToGroup(String nick, String group) {
      if (!RelayClient.isAuthed()) {
         ChatUtil.info("§d[Мессенджер] §cрелей недоступен — группы требуют подключения.");
         return;
      }
      RelayClient.send("gadd|" + RelayClient.enc(group) + "|" + RelayClient.enc(nick));
   }

   // --- GUI accessors -----------------------------------------------------

   public List<String> threadKeys() {
      List<String> out = new ArrayList<>();
      for (String g : this.groups) {
         out.add("#" + g);
      }
      for (String k : this.threads.keySet()) {
         if (!out.contains(k)) {
            out.add(k);
         }
      }
      return out;
   }

   public List<Msg> messages(String threadKey) {
      return this.thread(threadKey);
   }

   public int unreadOf(String threadKey) {
      return this.unread.getOrDefault(threadKey, 0);
   }

   public void openThread(String threadKey) {
      this.unread.remove(threadKey);
   }

   public boolean isOnline(String name) {
      for (String n : this.online) {
         if (n.equalsIgnoreCase(name)) {
            return true;
         }
      }
      return false;
   }

   public Set<String> knownGroups() {
      return this.groups;
   }

   public String myName() {
      return mc.field_1724 != null ? mc.field_1724.method_7334().name() : "";
   }

   // --- storage -----------------------------------------------------------

   private List<Msg> thread(String key) {
      return this.threads.computeIfAbsent(key, (k) -> new ArrayList<>());
   }

   private void persist(String threadKey, Msg m) {
      try {
         String line = m.ts + "|" + RelayClient.enc(threadKey) + "|" + RelayClient.enc(m.from) + "|" + (m.out ? 1 : 0) + "|" + RelayClient.enc(m.text) + System.lineSeparator();
         Files.writeString(LOG, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (Exception ignored) {
      }
   }

   private void loadHistory() {
      this.threads.clear();
      try {
         if (!Files.exists(LOG)) {
            return;
         }
         List<String> lines = Files.readAllLines(LOG, StandardCharsets.UTF_8);
         int from = Math.max(0, lines.size() - LOAD_LIMIT);
         for (int i = from; i < lines.size(); ++i) {
            String[] p = lines.get(i).split("\\|", -1);
            if (p.length >= 5) {
               this.thread(dec(p[1])).add(new Msg(dec(p[2]), dec(p[4]), Long.parseLong(p[0]), p[3].equals("1")));
            }
         }
      } catch (Exception ignored) {
      }
   }

   private static String dec(String s) {
      try {
         return URLDecoder.decode(s, StandardCharsets.UTF_8);
      } catch (Exception e) {
         return "";
      }
   }

   /** One chat message: author, text, timestamp, outgoing flag. */
   public static final class Msg {
      public final String from;
      public final String text;
      public final long ts;
      public final boolean out;

      public Msg(String from, String text, long ts, boolean out) {
         this.from = from;
         this.text = text;
         this.ts = ts;
         this.out = out;
      }
   }
}
