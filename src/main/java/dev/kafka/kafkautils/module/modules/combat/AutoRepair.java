package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.class_1268;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_1890;
import net.minecraft.class_6880;

/**
 * Mends Mending tools using a server XP mechanic: look straight up, hold sneak
 * and right-click to drop experience under you. Works through the whole
 * inventory — when the held item is full it swaps in the next damaged Mending
 * item (from the hotbar or main inventory), and once nothing is left below the
 * threshold it announces completion and stops, so Auto Mine / Auto Replant
 * (which pause via {@link #isBusy}) resume on their own.
 */
public class AutoRepair extends Module {
   private static final class_1268 MAIN_HAND = class_1268.values()[0];

   private final NumberSetting threshold = this.add(new NumberSetting("Repair Below %", 25, 1, 100, 5));
   private final NumberSetting clickInterval = this.add(new NumberSetting("Click Interval", 4, 1, 20, 1));
   private final BooleanSetting onlyMending = this.add(new BooleanSetting("Only Mending", true));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotations", true));

   private boolean active;
   private int clickCounter;
   private float savedPitch;
   private boolean savedView;
   private int savedSlot = -1;

   public AutoRepair() {
      super("Auto Repair", "Чинит предметы с Починкой опытом (взгляд вверх + шифт + ПКМ), сам берёт сломанные из инвентаря.", Category.FARMING);
   }

   protected void onDisable() {
      this.stop();
   }

   public boolean isBusy() {
      return this.active;
   }

   public void onTick() {
      if (mc.field_1724 == null || mc.field_1761 == null || mc.field_1690 == null || mc.field_1755 != null) {
         if (this.active) {
            this.stop();
         }
         return;
      }

      // While eating, release the crouch so it doesn't fight Auto Eat; keep the session.
      AutoEat eat = ModuleManager.get(AutoEat.class);
      if (eat != null && eat.isBusy()) {
         this.releaseHold();
         return;
      }

      if (!this.active) {
         if (!this.hasWork()) {
            return; // nothing below the threshold — stay idle so mining/farming runs
         }
         this.active = true;
         this.clickCounter = 0;
         this.savedSlot = mc.field_1724.method_31548().method_67532();
         this.savedView = false;
         if (!this.belowThreshold(mc.field_1724.method_6047())) {
            this.bringToHand();
         }
      } else {
         class_1799 held = mc.field_1724.method_6047();
         if (!this.isMendable(held) || isFull(held)) {
            if (!this.bringToHand()) {
               this.finish();
               return;
            }
         }
      }

      // Repair the held item.
      if (this.rotate.get()) {
         if (!this.savedView) {
            this.savedPitch = mc.field_1724.method_36455();
            this.savedView = true;
         }
         mc.field_1724.method_36457(-90.0F); // look straight up
      }
      mc.field_1690.field_1832.method_23481(true); // hold sneak (once, kept pressed)
      if (++this.clickCounter >= this.clickInterval.get()) {
         this.clickCounter = 0;
         mc.field_1761.method_2919(mc.field_1724, MAIN_HAND); // right-click to drop XP
      }
   }

   /** Selects the next damaged Mending item (hotbar first, then main inventory). */
   private boolean bringToHand() {
      for (int i = 0; i < 9; ++i) {
         if (this.belowThreshold(mc.field_1724.method_31548().method_5438(i))) {
            mc.field_1724.method_31548().method_61496(i);
            return true;
         }
      }
      for (int i = 9; i < 36; ++i) {
         if (this.belowThreshold(mc.field_1724.method_31548().method_5438(i))) {
            int sel = mc.field_1724.method_31548().method_67532();
            mc.field_1761.method_2906(mc.field_1724.field_7512.field_7763, i, sel, class_1713.field_7791, mc.field_1724);
            return true;
         }
      }
      return false;
   }

   private boolean hasWork() {
      for (int i = 0; i < 36; ++i) {
         if (this.belowThreshold(mc.field_1724.method_31548().method_5438(i))) {
            return true;
         }
      }
      return false;
   }

   private void finish() {
      ChatUtil.info("§d[Авторемонт] §aвсё починено.");
      this.stop();
   }

   private void stop() {
      this.releaseHold();
      if (this.savedView && mc.field_1724 != null) {
         mc.field_1724.method_36457(this.savedPitch);
      }
      if (this.savedSlot >= 0 && mc.field_1724 != null) {
         mc.field_1724.method_31548().method_61496(this.savedSlot);
      }
      this.savedView = false;
      this.savedSlot = -1;
      this.active = false;
      this.clickCounter = 0;
   }

   /** Releases the crouch without ending the session (used while paused for eating). */
   private void releaseHold() {
      if (mc.field_1690 != null) {
         mc.field_1690.field_1832.method_23481(false);
      }
   }

   /** Repairable (damageable) and, if required, carries Mending. */
   private boolean isMendable(class_1799 stack) {
      if (stack.method_7960() || !stack.method_7963()) {
         return false;
      }
      return !this.onlyMending.get() || hasMending(stack);
   }

   /** A repair target that is still worn below the trigger threshold. */
   private boolean belowThreshold(class_1799 stack) {
      if (!this.isMendable(stack)) {
         return false;
      }
      int max = stack.method_7936();
      if (max <= 0) {
         return false;
      }
      int pct = Math.round((float)(max - stack.method_7919()) / (float)max * 100.0F);
      return pct < this.threshold.get();
   }

   private static boolean isFull(class_1799 stack) {
      return stack.method_7919() == 0;
   }

   private static boolean hasMending(class_1799 stack) {
      for (Object2IntMap.Entry<class_6880<class_1887>> e : class_1890.method_57532(stack).method_57539()) {
         if (e.getKey().method_55840().endsWith("mending")) {
            return true;
         }
      }
      return false;
   }
}
