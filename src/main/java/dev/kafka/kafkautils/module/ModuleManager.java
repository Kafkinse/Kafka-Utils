package dev.kafka.kafkautils.module;

import dev.kafka.kafkautils.module.modules.chat.AntiSpam;
import dev.kafka.kafkautils.module.modules.chat.AutoTeleport;
import dev.kafka.kafkautils.module.modules.chat.ChatPing;
import dev.kafka.kafkautils.module.modules.chat.ClickableChat;
import dev.kafka.kafkautils.module.modules.chat.FriendChat;
import dev.kafka.kafkautils.module.modules.chat.FriendHighlight;
import dev.kafka.kafkautils.module.modules.chat.FriendList;
import dev.kafka.kafkautils.module.modules.chat.CoordinateShare;
import dev.kafka.kafkautils.module.modules.chat.Messenger;
import dev.kafka.kafkautils.module.modules.chat.ModRadar;
import dev.kafka.kafkautils.module.modules.chat.PlayerLogger;
import dev.kafka.kafkautils.module.modules.chat.PrivateMessages;
import dev.kafka.kafkautils.module.modules.chat.PlayerTracker;
import dev.kafka.kafkautils.module.modules.chat.StaffList;
import dev.kafka.kafkautils.module.modules.chat.StaffNotify;
import dev.kafka.kafkautils.module.modules.combat.ArmorAlert;
import dev.kafka.kafkautils.module.modules.combat.AutoRaid;
import dev.kafka.kafkautils.module.modules.combat.BrewHelper;
import dev.kafka.kafkautils.module.modules.combat.DpsMeter;
import dev.kafka.kafkautils.module.modules.combat.EnchantHelper;
import dev.kafka.kafkautils.module.modules.combat.EnemyStatus;
import dev.kafka.kafkautils.module.modules.combat.GappleCount;
import dev.kafka.kafkautils.module.modules.combat.AutoEat;
import dev.kafka.kafkautils.module.modules.combat.AutoMine;
import dev.kafka.kafkautils.module.modules.combat.AutoReplant;
import dev.kafka.kafkautils.module.modules.combat.AutoRepair;
import dev.kafka.kafkautils.module.modules.combat.KillDeathTracker;
import dev.kafka.kafkautils.module.modules.combat.MaceAlert;
import dev.kafka.kafkautils.module.modules.combat.SelfDurabilityAlert;
import dev.kafka.kafkautils.module.modules.combat.PearlTracker;
import dev.kafka.kafkautils.module.modules.combat.PvPLogger;
import dev.kafka.kafkautils.module.modules.combat.SplashPredictor;
import dev.kafka.kafkautils.module.modules.combat.TotemCounter;
import dev.kafka.kafkautils.module.modules.render.ActiveModsHud;
import dev.kafka.kafkautils.module.modules.render.ArmorDurabilityESP;
import dev.kafka.kafkautils.module.modules.render.EffectLogger;
import dev.kafka.kafkautils.module.modules.render.HealthESP;
import dev.kafka.kafkautils.module.modules.render.InventoryViewer;
import dev.kafka.kafkautils.module.modules.render.ItemESP;
import dev.kafka.kafkautils.module.modules.render.OminousVaultHighlighter;
import dev.kafka.kafkautils.module.modules.render.PinnedOrder;
import dev.kafka.kafkautils.module.modules.render.PlayerInspect;
import dev.kafka.kafkautils.module.modules.render.FriendOverlay;
import dev.kafka.kafkautils.module.modules.render.TrialSpawnerEsp;
import dev.kafka.kafkautils.module.modules.render.VanishESP;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
   private static final List<Module> MODULES = new ArrayList();

   private ModuleManager() {
   }

   public static void init() {
      register(new AutoRaid());
      register(new TotemCounter());
      register(new EnemyStatus());
      register(new PvPLogger());
      register(new GappleCount());
      register(new SplashPredictor());
      register(new ArmorAlert());
      register(new DpsMeter());
      register(new KillDeathTracker());
      register(new PearlTracker());
      register(new MaceAlert());
      register(new AutoMine());
      register(new AutoRepair());
      register(new AutoEat());
      register(new AutoReplant());
      register(new EnchantHelper());
      register(new BrewHelper());
      register(new SelfDurabilityAlert());
      register(new EffectLogger());
      register(new TrialSpawnerEsp());
      register(new OminousVaultHighlighter());
      register(new PlayerInspect());
      register(new VanishESP());
      register(new HealthESP());
      register(new ArmorDurabilityESP());
      register(new InventoryViewer());
      register(new ItemESP());
      register(new ActiveModsHud());
      register(new PinnedOrder());
      register(new FriendOverlay());
      register(new FriendList());
      register(new CoordinateShare());
      register(new FriendHighlight());
      register(new FriendChat());
      register(new PlayerLogger());
      register(new AutoTeleport());
      register(new ClickableChat());
      register(new PrivateMessages());
      register(new ModRadar());
      register(new Messenger());
      register(new StaffNotify());
      register(new PlayerTracker());
      register(new StaffList());
      register(new ChatPing());
      register(new AntiSpam());
   }

   private static void register(Module module) {
      MODULES.add(module);
   }

   public static List<Module> getModules() {
      return MODULES;
   }

   public static List<Module> getByCategory(Category category) {
      List<Module> result = new ArrayList();

      for(Module m : MODULES) {
         if (m.getCategory() == category) {
            result.add(m);
         }
      }

      return result;
   }

   public static void onTick() {
      for(Module m : MODULES) {
         if (m.isEnabled()) {
            try {
               m.onTick();
            } catch (Throwable t) {
               m.setEnabled(false);
               PrintStream var10000 = System.err;
               String var10001 = m.getName();
               var10000.println("[KafkaUtils] Disabled '" + var10001 + "' after a tick error: " + String.valueOf(t));
            }
         }
      }

   }

   public static <T extends Module> T get(Class<T> type) {
      for(Module m : MODULES) {
         if (type.isInstance(m)) {
            return (T)m;
         }
      }

      return null;
   }
}
