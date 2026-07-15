package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import net.minecraft.class_1268;
import net.minecraft.class_1799;

/**
 * Mends the held tool using a server XP mechanic where looking straight up while
 * sneaking and right-clicking drops experience under you (which Mending absorbs).
 * When the held tool drops below the threshold it aims up, holds sneak and
 * right-clicks for a while, then restores your view. Auto Mine pauses while this
 * runs (via {@link #isBusy}).
 */
public class AutoRepair extends Module {
   private static final class_1268 MAIN_HAND = class_1268.values()[0];

   private final NumberSetting threshold = this.add(new NumberSetting("Repair Below %", 25, 1, 100, 5));
   private final NumberSetting duration = this.add(new NumberSetting("Repair Duration", 60, 10, 200, 10));
   private final NumberSetting clickInterval = this.add(new NumberSetting("Click Interval", 4, 1, 20, 1));
   private final BooleanSetting onlyEnchanted = this.add(new BooleanSetting("Only Enchanted (Mending)", true));

   private int repairTicks;
   private int clickCounter;
   private float savedPitch;
   private boolean savedPitchValid;

   public AutoRepair() {
      super("Auto Repair", "Looks up + sneaks + right-clicks to drop XP and mend the held tool.", Category.FARMING);
   }

   protected void onDisable() {
      this.stop();
   }

   public boolean isBusy() {
      return this.repairTicks > 0;
   }

   public void onTick() {
      if (mc.field_1724 == null || mc.field_1761 == null || mc.field_1690 == null || mc.field_1755 != null) {
         return;
      }

      if (this.repairTicks > 0) {
         mc.field_1724.method_36457(-90.0F);              // look straight up
         mc.field_1690.field_1832.method_23481(true);     // hold sneak
         if (++this.clickCounter >= this.clickInterval.get()) {
            this.clickCounter = 0;
            mc.field_1761.method_2919(mc.field_1724, MAIN_HAND); // right-click (use item in air)
         }
         if (--this.repairTicks <= 0) {
            this.stop();
         }
         return;
      }

      // Yield to Auto Eat so we don't fight over the held item / use key.
      AutoEat eat = ModuleManager.get(AutoEat.class);
      if (eat != null && eat.isBusy()) {
         return;
      }

      if (this.needsRepair(mc.field_1724.method_6047())) {
         this.savedPitch = mc.field_1724.method_36455();
         this.savedPitchValid = true;
         this.repairTicks = this.duration.get();
         this.clickCounter = 0;
      }
   }

   private void stop() {
      if (mc.field_1690 != null) {
         mc.field_1690.field_1832.method_23481(false);
      }
      if (this.savedPitchValid && mc.field_1724 != null) {
         mc.field_1724.method_36457(this.savedPitch);
      }
      this.savedPitchValid = false;
      this.repairTicks = 0;
   }

   private boolean needsRepair(class_1799 stack) {
      if (stack.method_7960() || !stack.method_7963()) {
         return false;
      }
      if (this.onlyEnchanted.get() && !stack.method_7942()) {
         return false;
      }
      int max = stack.method_7936();
      if (max <= 0) {
         return false;
      }
      int pct = Math.round((float)(max - stack.method_7919()) / (float)max * 100.0F);
      return pct <= this.threshold.get();
   }
}
