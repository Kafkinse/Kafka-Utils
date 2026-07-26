package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import net.minecraft.class_1297;
import net.minecraft.class_1676;
import net.minecraft.class_1799;
import net.minecraft.class_243;
import net.minecraft.class_7923;

/**
 * Raises your shield automatically when a projectile is flying toward you, or
 * (optionally) right after you take a melee hit. It holds the use key while a
 * threat is active — so keep a shield in the off-hand and a non-usable item
 * (e.g. a sword) in the main hand for it to block reliably.
 *
 * <p>NOTE: input automation not exercisable in the build environment; mappings
 * verified for 1.21.11.
 */
public class AutoShield extends Module {
   private final NumberSetting range = this.add(new NumberSetting("Projectile Range", 8, 2, 24, 1));
   private final BooleanSetting blockMelee = this.add(new BooleanSetting("Block Melee", true));
   private final NumberSetting holdTicks = this.add(new NumberSetting("Hold Ticks", 6, 1, 40, 1));

   private int blockFor;
   private boolean holding;

   public AutoShield() {
      super("Auto Shield", "Авто-поднятие щита при летящем снаряде или после удара.", Category.COMBAT);
   }

   protected void onDisable() {
      this.releaseIfHeld();
   }

   public void onTick() {
      if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1690 == null) {
         return;
      }
      if (!this.hasShield()) {
         this.releaseIfHeld();
         return;
      }
      boolean threat = this.projectileThreat() || (this.blockMelee.get() && mc.field_1724.field_6235 > 0);
      if (threat) {
         this.blockFor = this.holdTicks.get();
      }
      if (this.blockFor > 0) {
         --this.blockFor;
         mc.field_1690.field_1904.method_23481(true); // hold use -> raise shield
         this.holding = true;
      } else {
         this.releaseIfHeld();
      }
   }

   private void releaseIfHeld() {
      if (this.holding && mc.field_1690 != null) {
         mc.field_1690.field_1904.method_23481(false);
         this.holding = false;
      }
   }

   private boolean projectileThreat() {
      class_243 me = mc.field_1724.method_61411();
      double r = this.range.get();
      double r2 = r * r;
      for (class_1297 e : mc.field_1687.method_18112()) {
         if (!(e instanceof class_1676) || e == mc.field_1724) {
            continue;
         }
         class_243 ep = e.method_61411();
         double dx = me.field_1352 - ep.field_1352;
         double dy = me.field_1351 - ep.field_1351;
         double dz = me.field_1350 - ep.field_1350;
         if (dx * dx + dy * dy + dz * dz > r2) {
            continue;
         }
         class_243 vel = e.method_18798();
         double vlen2 = vel.field_1352 * vel.field_1352 + vel.field_1351 * vel.field_1351 + vel.field_1350 * vel.field_1350;
         if (vlen2 < 0.02) {
            continue; // barely moving — not a real threat
         }
         // moving toward me: velocity dotted with the vector from projectile to me is positive.
         double dot = vel.field_1352 * dx + vel.field_1351 * dy + vel.field_1350 * dz;
         if (dot > 0.0) {
            return true;
         }
      }
      return false;
   }

   private boolean hasShield() {
      return this.isShield(mc.field_1724.method_6079()) || this.isShield(mc.field_1724.method_6047());
   }

   private boolean isShield(class_1799 s) {
      return !s.method_7960() && class_7923.field_41178.method_10221(s.method_7909()).method_12832().equals("shield");
   }
}
