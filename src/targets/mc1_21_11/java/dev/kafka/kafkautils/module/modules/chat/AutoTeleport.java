package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.config.ConfigManager;
import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.class_640;

/**
 * Auto-accepts teleport requests, but only from an allow-list of players. When a
 * received message looks like a teleport request (contains a keyword) and names
 * an allowed player, it runs the accept command. Manage the list from the menu
 * (opened by toggling this module) or with {@code /kafka tpa add|remove|list}.
 */
public class AutoTeleport extends Module {
   private final StringSetting playersSetting = this.add(new StringSetting("Players", ""));
   private final StringSetting acceptCmd = this.add(new StringSetting("Accept Command", "tpaccept"));
   private final StringSetting keywords = this.add(new StringSetting("Keywords",
      "телепорт,teleport,tpa,переместиться,хочет,wants,request,tpahere"));

   private static boolean openRequested;
   private long lastAccept;

   public AutoTeleport() {
      super("Auto Teleport", "Авто-приём телепорта от разрешённых игроков (/kafka tpa).", Category.CHAT);
   }

   protected void onEnable() {
      if (mc.field_1724 != null) {
         requestOpen();
      }
   }

   public static void requestOpen() {
      openRequested = true;
   }

   public static boolean consumeOpen() {
      boolean r = openRequested;
      openRequested = false;
      return r;
   }

   // --- allow-list --------------------------------------------------------

   public Set<String> allowed() {
      Set<String> out = new LinkedHashSet<>();
      for (String part : this.playersSetting.get().split("[,\\s]+")) {
         if (!part.isBlank()) {
            out.add(part.trim().toLowerCase(Locale.ROOT));
         }
      }
      return out;
   }

   public List<String> allowedList() {
      return new ArrayList<>(this.allowed());
   }

   public boolean isAllowed(String name) {
      return name != null && this.allowed().contains(name.toLowerCase(Locale.ROOT));
   }

   public boolean add(String name) {
      if (name == null || name.isBlank() || this.isAllowed(name)) {
         return false;
      }
      Set<String> s = this.allowed();
      s.add(name.trim().toLowerCase(Locale.ROOT));
      this.save(s);
      return true;
   }

   public boolean remove(String name) {
      if (name == null || !this.isAllowed(name)) {
         return false;
      }
      Set<String> s = this.allowed();
      s.remove(name.toLowerCase(Locale.ROOT));
      this.save(s);
      return true;
   }

   private void save(Set<String> s) {
      this.playersSetting.set(String.join(",", s));
      ConfigManager.save();
   }

   /** Names currently in the tab list (for the menu and command suggestions). */
   public List<String> onlinePlayers() {
      List<String> out = new ArrayList<>();
      if (mc.method_1562() != null) {
         for (class_640 entry : mc.method_1562().method_2880()) {
            if (entry.method_2966() != null && entry.method_2966().name() != null) {
               out.add(entry.method_2966().name());
            }
         }
      }
      Collections.sort(out);
      return out;
   }

   // --- request handling --------------------------------------------------

   /** Inspects an incoming chat line; accepts if it is a request from an allowed player. */
   public void handleMessage(String text) {
      if (!this.isEnabled() || mc.method_1562() == null || text == null) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - this.lastAccept < 3000L) {
         return; // debounce duplicate/echoed request lines
      }
      String lower = text.toLowerCase(Locale.ROOT);
      if (!this.hasKeyword(lower)) {
         return;
      }
      String requester = this.matchedAllowed(text);
      if (requester == null) {
         return;
      }
      String cmd = this.acceptCmd.get().trim();
      if (cmd.isEmpty()) {
         cmd = "tpaccept";
      }
      cmd = cmd.replace("{player}", requester);
      if (cmd.startsWith("/")) {
         cmd = cmd.substring(1);
      }
      mc.method_1562().method_45730(cmd);
      ChatUtil.info("§d[TPA] §aпринят телепорт от §r" + requester);
      this.lastAccept = now;
   }

   private boolean hasKeyword(String lower) {
      for (String kw : this.keywords.get().split("[,]+")) {
         String k = kw.trim().toLowerCase(Locale.ROOT);
         if (!k.isEmpty() && lower.contains(k)) {
            return true;
         }
      }
      return false;
   }

   /** @return the allowed player named in the text (original case), or null. */
   private String matchedAllowed(String text) {
      Set<String> allow = this.allowed();
      for (String word : text.split("[^A-Za-z0-9_]+")) {
         if (!word.isEmpty() && allow.contains(word.toLowerCase(Locale.ROOT))) {
            return word;
         }
      }
      return null;
   }
}
