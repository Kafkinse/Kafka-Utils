package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_1799;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_742;

/**
 * Warns when a nearby enemy holding a mace is winding up (rising) or committing
 * to a downward slam, escalating SAFE → WATCH → DANGER → IMPACT. Rewritten to fit
 * the mod's actual module framework (the original file targeted a different, and
 * non-existent, Module/Setting API and yarn-named Minecraft classes).
 */
public class MaceAlert extends Module implements HudModule {
   private final NumberSetting minHeight = this.add(new NumberSetting("Min Height", 8, 3, 20, 1));
   private final NumberSetting detectionRange = this.add(new NumberSetting("Detection Range", 20, 10, 40, 2));
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));
   private final BooleanSetting showDamage = this.add(new BooleanSetting("Show Damage Est", true));

   private enum ThreatLevel {
      SAFE,
      WATCH,
      DANGER,
      IMPACT
   }

   private ThreatLevel currentThreat = ThreatLevel.SAFE;
   private String threatName = null;
   private float estimatedDamage = 0.0F;

   public MaceAlert() {
      super("Mace Alert", "Warns when an enemy is winding up a mace slam.", Category.COMBAT);
   }

   protected void onEnable() {
      this.reset();
   }

   protected void onDisable() {
      this.reset();
   }

   private void reset() {
      this.currentThreat = ThreatLevel.SAFE;
      this.threatName = null;
      this.estimatedDamage = 0.0F;
   }

   public void onTick() {
      if (mc.field_1687 == null || mc.field_1724 == null) {
         return;
      }

      double rangeSq = (double)(this.detectionRange.get() * this.detectionRange.get());
      ThreatLevel best = ThreatLevel.SAFE;
      String bestName = null;
      float bestDamage = 0.0F;

      for (class_742 p : mc.field_1687.method_18456()) {
         if (p == mc.field_1724 || mc.field_1724.method_5858(p) > rangeSq) {
            continue;
         }
         ThreatLevel level = this.analyze(p);
         if (level.ordinal() > best.ordinal()) {
            best = level;
            bestName = p.method_5477().getString();
            bestDamage = this.estimateDamage(p);
         }
      }

      ThreatLevel previous = this.currentThreat;
      this.currentThreat = best;
      this.threatName = bestName;
      this.estimatedDamage = bestDamage;

      if (this.sound.get() && best != ThreatLevel.SAFE && best.ordinal() > previous.ordinal()) {
         float pitch = 1.0F + (float)best.ordinal() * 0.25F;
         mc.field_1724.method_5783((class_3414)class_3417.field_14622.comp_349(), 0.7F, pitch);
      }
   }

   private ThreatLevel analyze(class_742 attacker) {
      if (!this.hasMace(attacker)) {
         return ThreatLevel.SAFE;
      }

      double velocityY = attacker.method_18798().field_1351;
      double heightAbove = attacker.method_61411().field_1351 - mc.field_1724.method_61411().field_1351;
      double distance = Math.sqrt(mc.field_1724.method_5858(attacker));

      if (velocityY < -0.5 && heightAbove > 0.5 && distance < 8.0) {
         return ThreatLevel.IMPACT;
      }
      if (velocityY < -0.3 && heightAbove > (double)this.minHeight.get()) {
         return ThreatLevel.DANGER;
      }
      if (velocityY > 0.15) {
         return ThreatLevel.WATCH;
      }
      return ThreatLevel.SAFE;
   }

   private boolean hasMace(class_742 player) {
      return isMace(player.method_6047()) || isMace(player.method_6079());
   }

   private static boolean isMace(class_1799 stack) {
      return !stack.method_7960()
         && stack.method_7909().method_63680().method_63700().toLowerCase(Locale.ROOT).contains("mace");
   }

   private float estimateDamage(class_742 attacker) {
      double speed = attacker.method_18798().method_1033();
      return (float)(9.0 + speed * 5.0);
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList<>();
      if (this.currentThreat == ThreatLevel.SAFE || this.threatName == null) {
         lines.add("§aБезопасно");
      } else {
         lines.add(this.threatColor() + this.currentThreat.name());
         lines.add("§7Игрок: §r" + this.threatName);
         if (this.showDamage.get()) {
            lines.add(String.format(Locale.ROOT, "§7Урон ~§c%.1f", this.estimatedDamage));
         }
      }

      return RenderUtil.panel(ctx, x, y, "Mace Alert", lines);
   }

   private String threatColor() {
      switch (this.currentThreat) {
         case WATCH:
            return "§e";
         case DANGER:
            return "§6";
         case IMPACT:
            return "§c§l";
         default:
            return "§a";
      }
   }
}
