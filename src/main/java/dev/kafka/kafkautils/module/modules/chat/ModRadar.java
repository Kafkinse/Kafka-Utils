package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Tracks which players also run Kafka-Utils and marks them with a §d✦ badge in
 * the tab list (via {@code PlayerListHudMixin}). Detection is passive (any
 * friend-chat message proves the sender has the mod) plus an active handshake:
 * shortly after joining, a hidden PING whisper goes to online friends and their
 * mod replies PONG — both lines are swallowed, players never see them.
 */
public class ModRadar extends Module {
   /**
    * Control payload prefix inside the friend-chat marker protocol. Must be
    * printable ASCII: these payloads travel through the server's whisper
    * command, and vanilla rejects control characters ("Invalid characters in
    * chat") and the section sign, which kicks the sender.
    */
   public static final String CONTROL = "~kfc~";

   private static final Set<String> USERS = new HashSet<>();

   private final BooleanSetting tabBadge = this.add(new BooleanSetting("Tab Badge", true));
   private final BooleanSetting handshake = this.add(new BooleanSetting("Handshake", true));
   private final BooleanSetting announce = this.add(new BooleanSetting("Announce", true));

   private final Set<String> ponged = new HashSet<>();
   private Object lastHandler;
   private int pingDelay = -1;

   public ModRadar() {
      super("Mod Radar", "Помечает в табе игроков с Kafka-Utils (значок ✦).", Category.FRIENDS);
   }

   protected void onDisable() {
      USERS.clear();
      this.ponged.clear();
   }

   public static boolean isUser(String name) {
      return name != null && USERS.contains(name.toLowerCase(Locale.ROOT));
   }

   /** Used by the tab-list mixin. */
   public static boolean shouldBadge(String name) {
      ModRadar r = ModuleManager.get(ModRadar.class);
      return r != null && r.isEnabled() && r.tabBadge.get() && isUser(name);
   }

   /** Any decoded friend-chat traffic from {@code name} proves they run the mod. */
   public void markUser(String name) {
      if (name == null || name.isBlank()) {
         return;
      }
      if (USERS.add(name.toLowerCase(Locale.ROOT)) && this.announce.get()) {
         ChatUtil.info("§d✦ §r" + name + " §7использует Kafka-Utils");
      }
   }

   /** Hidden control payload (PING/PONG) received through Friend Chat. */
   public void onControl(String sender, String cmd) {
      this.markUser(sender);
      if (cmd.equals("PING") && this.ponged.add(sender.toLowerCase(Locale.ROOT))) {
         FriendChat fc = ModuleManager.get(FriendChat.class);
         if (fc != null) {
            fc.whisper(sender, CONTROL + "PONG");
         }
      }
   }

   public void onTick() {
      Object nh = mc.method_1562();
      if (nh == null) {
         return;
      }
      if (nh != this.lastHandler) {
         // New connection: forget stale detections and schedule a handshake.
         this.lastHandler = nh;
         USERS.clear();
         this.ponged.clear();
         this.pingDelay = 100; // ~5s after join, so the tab list is populated
         return;
      }
      if (this.pingDelay > 0) {
         --this.pingDelay;
      } else if (this.pingDelay == 0) {
         this.pingDelay = -1;
         if (this.handshake.get()) {
            FriendChat fc = ModuleManager.get(FriendChat.class);
            FriendList list = ModuleManager.get(FriendList.class);
            if (fc != null && list != null) {
               for (String friend : list.onlineFriends()) {
                  fc.whisper(friend, CONTROL + "PING");
               }
            }
         }
      }
   }
}
