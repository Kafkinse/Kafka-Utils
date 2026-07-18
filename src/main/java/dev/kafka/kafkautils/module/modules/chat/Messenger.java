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
import java.util.Random;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_3414;
import net.minecraft.class_3417;

/**
 * Discord-style messenger between Kafka-Utils users. Primary transport is the
 * relay server ({@link RelayClient}) — messages never touch the Minecraft
 * server. When the relay is unreachable, direct messages fall back to hidden
 * marker whispers through Friend Chat (the game server sees those). Threads:
 * {@code @nick} for DMs and {@code #group} for relay-side groups.
 *
 * <p>Each message carries a short id so edits and deletes can be propagated to
 * the other side. Edits/deletes apply in-memory for the current session; the
 * on-disk history keeps the original text.
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
   private static final long TYPING_TTL = 4000L;   // how long "typing…" stays after the last signal
   private static final long TYPING_SEND = 1500L;  // min gap between outgoing typing signals
   private static final Random RNG = new Random();
   private static boolean openRequested;

   private final StringSetting url = this.add(new StringSetting("Server URL", "ws://163.123.180.81:10041"));
   private final StringSetting key = this.add(new StringSetting("Key", "changeme"));
   private final BooleanSetting fallback = this.add(new BooleanSetting("Fallback via /msg", true));
   private final BooleanSetting notify = this.add(new BooleanSetting("Chat Notify", true));
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));

   private final Map<String, List<Msg>> threads = new LinkedHashMap<>();
   private final Map<String, Integer> unread = new LinkedHashMap<>();
   private final Set<String> online = new LinkedHashSet<>();
   private final Set<String> groups = new LinkedHashSet<>();
   private final Map<String, List<String>> groupMembers = new LinkedHashMap<>();
   private final Map<String, String> typingBy = new LinkedHashMap<>();
   private final Map<String, Long> typingUntil = new LinkedHashMap<>();
   private final Map<String, Long> lastTypingSent = new LinkedHashMap<>();
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
            // msg|from|text|ts|id
            if (p.length >= 3) {
               String from = dec(p[1]);
               String id = p.length >= 5 ? p[4] : this.newId();
               if (!from.equalsIgnoreCase(this.myName())) {
                  this.incoming("@" + from, from, dec(p[2]), id);
               }
            }
         }
         case "gmsg" -> {
            // gmsg|group|from|text|ts|id
            if (p.length >= 4) {
               String group = dec(p[1]);
               String from = dec(p[2]);
               String id = p.length >= 6 ? p[5] : this.newId();
               this.groups.add(group);
               if (!from.equalsIgnoreCase(this.myName())) {
                  this.incoming("#" + group, from, dec(p[3]), id);
               }
            }
         }
         case "gsys" -> {
            if (p.length >= 3) {
               String group = dec(p[1]);
               this.groups.add(group);
               this.thread("#" + group);
               String text = dec(p[2]);
               Msg m = new Msg(this.newId(), "§7*", text, System.currentTimeMillis(), false);
               this.thread("#" + group).add(m);
               this.persist("#" + group, m);
               if (this.notify.get()) {
                  ChatUtil.raw("§d✦ §7(#" + group + ") " + text);
               }
            }
         }
         case "del" -> {
            // del|threadKey|id
            if (p.length >= 3) {
               Msg m = this.findById(dec(p[1]), p[2]);
               if (m != null) {
                  m.deleted = true;
               }
            }
         }
         case "edt" -> {
            // edt|threadKey|id|text
            if (p.length >= 4) {
               Msg m = this.findById(dec(p[1]), p[2]);
               if (m != null) {
                  m.text = dec(p[3]);
                  m.edited = true;
               }
            }
         }
         case "typ" -> {
            // typ|threadKey|who
            if (p.length >= 3) {
               String tk = dec(p[1]);
               this.typingBy.put(tk, dec(p[2]));
               this.typingUntil.put(tk, System.currentTimeMillis() + TYPING_TTL);
            }
         }
         case "gmembers" -> {
            // gmembers|group|a,b,c
            if (p.length >= 3) {
               List<String> list = new ArrayList<>();
               for (String n : dec(p[2]).split(",")) {
                  if (!n.isBlank()) {
                     list.add(n);
                  }
               }
               this.groupMembers.put(dec(p[1]), list);
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
                     this.thread("#" + g);
                  }
               }
            }
         }
         default -> { }
      }
   }

   /** A fallback whisper (via Friend Chat) routed into the messenger window. */
   public void onFallback(String sender, String text) {
      this.incoming("@" + sender, sender, text, this.newId());
   }

   private void incoming(String threadKey, String from, String text, String id) {
      Msg m = new Msg(id, from, text, System.currentTimeMillis(), false);
      this.thread(threadKey).add(m);
      this.persist(threadKey, m);
      this.unread.merge(threadKey, 1, Integer::sum);
      this.typingBy.remove(threadKey); // a message arriving clears "typing…"
      if (this.notify.get()) {
         ChatUtil.raw("§d✉ §b" + from + (threadKey.startsWith("#") ? " §7(" + threadKey + ")" : "") + "§7: §r" + text);
      }
      if (this.sound.get() && mc.field_1724 != null) {
         mc.field_1724.method_5783((class_3414) class_3417.field_14622.comp_349(), 0.7F, 1.6F);
      }
   }

   // --- outbound ----------------------------------------------------------

   /** Sends into a thread; returns a short status note for the GUI. */
   public String send(String threadKey, String text) {
      text = text.trim();
      if (text.isEmpty()) {
         return "";
      }
      String id = this.newId();
      Msg m = new Msg(id, this.myName(), text, System.currentTimeMillis(), true);
      this.thread(threadKey).add(m);
      this.persist(threadKey, m);

      if (RelayClient.isAuthed()) {
         if (threadKey.startsWith("@")) {
            RelayClient.send("msg|" + RelayClient.enc(threadKey.substring(1)) + "|" + RelayClient.enc(text) + "|" + id);
         } else {
            RelayClient.send("gmsg|" + RelayClient.enc(threadKey.substring(1)) + "|" + RelayClient.enc(text) + "|" + id);
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

   /** Deletes one of your own messages locally and on the other side. */
   public void deleteMessage(String threadKey, String id) {
      Msg m = this.findById(threadKey, id);
      if (m == null || !m.out) {
         return;
      }
      m.deleted = true;
      if (RelayClient.isAuthed()) {
         RelayClient.send("del|" + RelayClient.enc(threadKey) + "|" + id);
      }
   }

   /** Edits one of your own messages locally and on the other side. */
   public void editMessage(String threadKey, String id, String newText) {
      newText = newText.trim();
      Msg m = this.findById(threadKey, id);
      if (m == null || !m.out || newText.isEmpty()) {
         return;
      }
      m.text = newText;
      m.edited = true;
      if (RelayClient.isAuthed()) {
         RelayClient.send("edt|" + RelayClient.enc(threadKey) + "|" + id + "|" + RelayClient.enc(newText));
      }
   }

   /** Signals to the thread's peers that we are typing (throttled). */
   public void sendTyping(String threadKey) {
      if (threadKey == null || !RelayClient.isAuthed()) {
         return;
      }
      long now = System.currentTimeMillis();
      Long last = this.lastTypingSent.get(threadKey);
      if (last != null && now - last < TYPING_SEND) {
         return;
      }
      this.lastTypingSent.put(threadKey, now);
      RelayClient.send("typ|" + RelayClient.enc(threadKey));
   }

   public void createGroup(String name) {
      if (!RelayClient.isAuthed()) {
         ChatUtil.info("§d[Мессенджер] §cрелей недоступен — группы требуют подключения.");
         return;
      }
      RelayClient.send("gcreate|" + RelayClient.enc(name));
      this.groups.add(name);
      this.thread("#" + name);
   }

   public void addToGroup(String nick, String group) {
      if (!RelayClient.isAuthed()) {
         ChatUtil.info("§d[Мессенджер] §cрелей недоступен — группы требуют подключения.");
         return;
      }
      RelayClient.send("gadd|" + RelayClient.enc(group) + "|" + RelayClient.enc(nick));
   }

   /** Leaves a group; the relay drops us and tells the rest. */
   public void leaveGroup(String group) {
      if (RelayClient.isAuthed()) {
         RelayClient.send("gleave|" + RelayClient.enc(group));
      }
      this.groups.remove(group);
      this.groupMembers.remove(group);
      this.threads.remove("#" + group);
      this.unread.remove("#" + group);
   }

   /** Asks the relay for the current member list of a group. */
   public void requestMembers(String group) {
      if (RelayClient.isAuthed()) {
         RelayClient.send("gwho|" + RelayClient.enc(group));
      }
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

   public List<String> membersOf(String group) {
      return this.groupMembers.getOrDefault(group, List.of());
   }

   /** "X печатает…" for the thread, or null if nobody is typing right now. */
   public String typingText(String threadKey) {
      Long until = this.typingUntil.get(threadKey);
      if (until == null || System.currentTimeMillis() > until) {
         return null;
      }
      String who = this.typingBy.get(threadKey);
      return who == null ? null : who + " печатает…";
   }

   /** Searches all threads for messages containing the query. */
   public List<String> search(String query) {
      List<String> out = new ArrayList<>();
      String q = query.toLowerCase();
      for (Map.Entry<String, List<Msg>> e : this.threads.entrySet()) {
         for (Msg m : e.getValue()) {
            if (!m.deleted && m.text.toLowerCase().contains(q)) {
               out.add("§7" + e.getKey() + " §b" + m.from + "§7: §r" + m.text);
            }
         }
      }
      return out;
   }

   public String myName() {
      return mc.field_1724 != null ? mc.field_1724.method_7334().name() : "";
   }

   // --- storage -----------------------------------------------------------

   private List<Msg> thread(String key) {
      return this.threads.computeIfAbsent(key, (k) -> new ArrayList<>());
   }

   private Msg findById(String threadKey, String id) {
      List<Msg> list = this.threads.get(threadKey);
      if (list != null) {
         for (Msg m : list) {
            if (m.id.equals(id)) {
               return m;
            }
         }
      }
      return null;
   }

   private String newId() {
      return Long.toString(System.nanoTime(), 36) + Integer.toString(RNG.nextInt(1296), 36);
   }

   private void persist(String threadKey, Msg m) {
      try {
         String line = m.ts + "|" + RelayClient.enc(threadKey) + "|" + RelayClient.enc(m.from) + "|" + (m.out ? 1 : 0)
            + "|" + RelayClient.enc(m.text) + "|" + m.id + System.lineSeparator();
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
               String id = p.length >= 6 ? p[5] : this.newId();
               this.thread(dec(p[1])).add(new Msg(id, dec(p[2]), dec(p[4]), Long.parseLong(p[0]), p[3].equals("1")));
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

   /** One chat message: id, author, text (mutable for edits), timestamp, flags. */
   public static final class Msg {
      public final String id;
      public final String from;
      public String text;
      public final long ts;
      public final boolean out;
      public boolean deleted;
      public boolean edited;

      public Msg(String id, String from, String text, long ts, boolean out) {
         this.id = id;
         this.from = from;
         this.text = text;
         this.ts = ts;
         this.out = out;
      }
   }
}
