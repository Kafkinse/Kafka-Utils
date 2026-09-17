package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.class_640;

/**
 * Logs players joining/leaving the server. Silently re-syncs when the connection
 * changes (first join or a proxy server switch) so the whole tab list is not
 * announced as "joined", absorbs mass tab-list churn, and ignores NPC entries.
 */
public class PlayerLogger extends Module {
   private static final int BURST = 4;

   private final BooleanSetting ignoreNpc = this.add(new BooleanSetting("Ignore NPCs", true));
   private final Set<String> known = new HashSet<>();
   private Object lastHandler;
   private int settleTicks;

   public PlayerLogger() {
      super("Player Logger", "Logs players joining/leaving (skips join spam and NPCs).", Category.CHAT);
   }

   protected void onEnable() {
      this.known.clear();
      this.lastHandler = null; // force a silent re-seed on the next tick
      this.settleTicks = 0;
   }

   protected void onDisable() {
      this.known.clear();
      this.lastHandler = null;
   }

   public void onTick() {
      Object nh = mc.method_1562();
      if (nh == null) {
         return;
      }

      Set<String> current = new HashSet<>();
      for (class_640 entry : mc.method_1562().method_2880()) {
         if (this.isRealPlayer(entry)) {
            current.add(entry.method_2966().name());
         }
      }

      // New connection (first join or proxy server switch): seed silently.
      if (nh != this.lastHandler) {
         this.lastHandler = nh;
         this.known.clear();
         this.known.addAll(current);
         this.settleTicks = 0;
         return;
      }

      List<String> joined = new ArrayList<>();
      List<String> left = new ArrayList<>();
      for (String name : current) {
         if (!this.known.contains(name)) {
            joined.add(name);
         }
      }
      for (String name : this.known) {
         if (!current.contains(name)) {
            left.add(name);
         }
      }

      // Absorb mass churn (server switch / resync spread over a few ticks).
      if (this.settleTicks > 0) {
         --this.settleTicks;
      } else if (joined.size() + left.size() > BURST) {
         this.settleTicks = 40;
      } else {
         for (String name : joined) {
            ChatUtil.raw("§a+ §r" + name + "§a зашёл");
         }
         for (String name : left) {
            ChatUtil.raw("§c- §r" + name + "§c вышел");
         }
      }

      this.known.clear();
      this.known.addAll(current);
   }

   /** A listed player with a name, excluding Citizens-style NPCs (version-2 UUID). */
   private boolean isRealPlayer(class_640 entry) {
      if (entry.method_2966() == null || entry.method_2966().name() == null) {
         return false;
      }
      return !this.ignoreNpc.get() || entry.method_2966().id().version() != 2;
   }
}
