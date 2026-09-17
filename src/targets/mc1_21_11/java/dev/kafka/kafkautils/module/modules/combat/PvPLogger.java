package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_742;

public class PvPLogger extends Module {
   private final BooleanSetting logConsumables = this.add(new BooleanSetting("Log Potions/Apples", true));
   private final BooleanSetting logTotems = this.add(new BooleanSetting("Log Totem Pops", true));

   private final Map<UUID, Boolean> wasUsing = new HashMap();
   private final Map<UUID, Integer> lastTimeLeft = new HashMap();
   private final Map<UUID, class_1792> lastItem = new HashMap();

   public PvPLogger() {
      super("PvP Logger", "Logs consumed potions/apples and totem pops.", Category.COMBAT);
   }

   /** Called by Totem Counter when a totem pops. Writes "<name> потерял тотем" to chat. */
   public void logTotem(String name) {
      if (this.isEnabled() && this.logTotems.get()) {
         ChatUtil.raw("§5" + name + "§r потерял тотем");
      }
   }

   protected void onEnable() {
      this.wasUsing.clear();
      this.lastTimeLeft.clear();
      this.lastItem.clear();
   }

   public void onTick() {
      if (mc.field_1687 != null && mc.field_1724 != null) {
         for(class_742 p : mc.field_1687.method_18456()) {
            if (p != mc.field_1724) {
               UUID id = p.method_5667();
               boolean using = p.method_6115();
               if (using) {
                  this.lastItem.put(id, p.method_6030().method_7909());
                  this.lastTimeLeft.put(id, p.method_6014());
               }

               boolean prev = (Boolean)this.wasUsing.getOrDefault(id, false);
               if (prev && !using) {
                  int tl = (Integer)this.lastTimeLeft.getOrDefault(id, 99);
                  class_1792 it = (class_1792)this.lastItem.get(id);
                  if (this.logConsumables.get() && tl <= 2 && it != null && isConsumable(it)) {
                     String var10000 = p.method_5477().getString();
                     ChatUtil.raw("§d" + var10000 + "§r использовал §d" + it.method_63680().getString());
                  }
               }

               this.wasUsing.put(id, using);
            }
         }

      }
   }

   private static boolean isConsumable(class_1792 i) {
      return i == class_1802.field_8574 || i == class_1802.field_8463 || i == class_1802.field_8367 || i == class_1802.field_8103 || i == class_1802.field_20417;
   }
}
