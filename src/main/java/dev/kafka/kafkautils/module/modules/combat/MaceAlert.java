package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.WorldRenderModule;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.setting.ModeSetting;
import dev.kafka.kafkautils.util.Render3D;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_742;

public class MaceAlert extends Module implements WorldRenderModule, HudModule {
   // Settings
   private final NumberSetting minHeight = (NumberSetting)this.add(new NumberSetting("Min Height", 8, 3, 50, 1));
   private final NumberSetting detectRange = (NumberSetting)this.add(new NumberSetting("Detection Range", 20, 5, 50, 5));
   private final ModeSetting alertLevel = (ModeSetting)this.add(new ModeSetting("Alert Level", 0, "WATCH", "DANGER", "IMPACT"));
   private final BooleanSetting soundEnabled = (BooleanSetting)this.add(new BooleanSetting("Sound", true));
   private final BooleanSetting autoShield = (BooleanSetting)this.add(new BooleanSetting("Auto Shield", true));
   private final BooleanSetting showTimer = (BooleanSetting)this.add(new BooleanSetting("Show Timer", true));
   private final BooleanSetting showDamageEst = (BooleanSetting)this.add(new BooleanSetting("Show Damage Est", true));
   private final BooleanSetting dashAway = (BooleanSetting)this.add(new BooleanSetting("Dash Away", false));

   // Sound state
   private int soundCooldown = 0;
   private String lastLevel = "";

   // Tracking
   private final List<Threat> threats = new ArrayList();

   public MaceAlert() {
      super("Mace Alert", "Detects mace-wielding enemies and predicts impact.", Category.COMBAT);
   }

   protected void onEnable() {
      this.threats.clear();
      this.soundCooldown = 0;
   }

   public void onTick() {
      if (mc.field_1687 == null || mc.field_1724 == null) return;
      this.threats.clear();

      if (this.soundCooldown > 0) this.soundCooldown--;

      int range = this.detectRange.get();
      int minH = this.minHeight.get();
      class_243 myPos = mc.field_1724.method_73189();

      String highestLevel = "WATCH";
      class_742 highestThreat = null;
      double highestDist = (double)range;

      for (class_742 p : mc.field_1687.method_18456()) {
         if (p == mc.field_1724) continue;

         class_1799 mainHand = p.method_6047();
         boolean hasMace = !mainHand.method_7960() && mainHand.method_7909() == class_1802.field_49814;
         if (!hasMace) continue;

         class_243 pPos = p.method_73189();
         double dist = myPos.method_5882(pPos);
         if (dist > (double)range) continue;

         class_243 vel = p.method_18800();
         double yVel = vel.field_1351;
         double heightAboveMe = pPos.field_1351 - myPos.field_1351;

         String level = "WATCH";
         int alertColor = 0xFFFFAA00;

         // WATCH: enemy going up with mace
         if (yVel > 0.1 && heightAboveMe < 5.0) {
            level = "WATCH";
            alertColor = 0xFFFFAA00;
         }

         // DANGER: enemy falling while looking at us
         if (yVel < -0.3 && heightAboveMe > (double)minH) {
            level = "DANGER";
            alertColor = 0xFFFF6600;
         }

         // IMPACT: close and falling fast
         if (yVel < -0.5 && dist < 12.0 && heightAboveMe > (double)minH) {
            level = "IMPACT";
            alertColor = 0xFFFF0000;
         }

         // Track highest level for sound triggering
         if (level.equals("IMPACT") || (level.equals("DANGER") && !highestLevel.equals("IMPACT"))) {
            highestLevel = level;
            highestThreat = p;
            highestDist = dist;
         }

         // Auto shield
         if (autoShield.get() && level.equals("IMPACT") && !mc.field_1724.method_5715()) {
            // Equip shield
            for (int i = 0; i < 9; i++) {
               class_1799 stack = mc.field_1724.method_7371().method_5438(i);
               if (!stack.method_7960() && stack.method_7909() == class_1802.field_8225) {
                  mc.field_1724.method_7371().field_7545 = i;
                  break;
               }
            }
         }

         // Dash away
         if (this.dashAway.get() && level.equals("IMPACT")) {
            double strafeX = (Math.random() - 0.5) * 0.6;
            double strafeZ = (Math.random() - 0.5) * 0.6;
            mc.field_1724.method_18718().method_18721(
               mc.field_1724.method_18718().field_1326 + strafeX,
               mc.field_1724.method_18718().field_1327,
               mc.field_1724.method_18718().field_1328 + strafeZ
            );
         }

         // Calculate estimated damage
         double fallDist = Math.max(0.0, (pPos.field_1351 - myPos.field_1351 - 1.0));
         double estimatedDmg = Math.min(80.0, fallDist * 1.5 + 5.0);
         long timeToImpact = (long)(dist / Math.max(0.1, Math.abs(yVel)) * 2.0);

         this.threats.add(new Threat(p, level, alertColor, estimatedDmg, timeToImpact, vel, pPos));
      }

      // === SOUND ENGINE ===
      if (soundEnabled.get() && highestThreat != null && this.soundCooldown <= 0) {
         this.playRadarSound(highestLevel, highestThreat, highestDist, range);
         this.lastLevel = highestLevel;
      }
   }

   /**
    * Plays a radar-style sound that varies by threat level and proximity.
    * 3 different sounds + pitch rises as threat gets closer (like a radar).
    */
   private void playRadarSound(String level, class_742 threat, double dist, int range) {
      double prox = Math.max(0.0, Math.min(1.0, 1.0 - (dist / (double)range)));

      class_3414 sound;
      float basePitch;
      float volume;

      switch (level) {
         case "IMPACT" -> {
            // Urgent drum hit — snare/bass drum
            sound = (class_3414)class_3417.field_14883.comp_349();
            basePitch = 1.2F;
            volume = 0.8F;
            this.soundCooldown = 15; // every 0.75s
         }
         case "DANGER" -> {
            // Clear ping alert — note block pling
            sound = (class_3414)class_3417.field_14869.comp_349();
            basePitch = 0.8F;
            volume = 0.6F;
            this.soundCooldown = 25; // every 1.25s
         }
         default -> {
            // WATCH: soft orb pickup — subtle awareness
            sound = (class_3414)class_3417.field_14622.comp_349();
            basePitch = 0.5F;
            volume = 0.35F;
            this.soundCooldown = 40; // every 2s
         }
      }

      // Pitch rises with proximity (radar effect!)
      // WATCH: 0.50 → 0.90  |  DANGER: 0.80 → 1.40  |  IMPACT: 1.20 → 2.00
      float pitchRange = switch (level) {
         case "IMPACT" -> 0.8F;
         case "DANGER" -> 0.6F;
         default -> 0.4F;
      };
      float pitch = basePitch + (float)prox * pitchRange;

      // Clamp pitch to Minecraft's valid range
      pitch = Math.max(0.5F, Math.min(2.0F, pitch));

      // Play the sound
      mc.field_1724.method_5783(sound, volume, pitch);
   }

   public void onWorldRender(WorldRenderContext ctx) {
      if (mc.field_1724 == null) return;
      class_243 myPos = mc.field_1724.method_33571();

      for (Threat t : this.threats) {
         // Arrow pointing toward the threat
         class_243 dir = new class_243(
            t.pos.field_1352 - myPos.field_1352,
            0.0,
            t.pos.field_1350 - myPos.field_1350
         ).method_1029();

         double arrowLen = 1.5;
         class_243 arrowEnd = new class_243(
            myPos.field_1352 + dir.field_1352 * arrowLen,
            myPos.field_1351,
            myPos.field_1350 + dir.field_1350 * arrowLen
         );
         Render3D.line(myPos, arrowEnd, 0x99FFFFFF);
         Render3D.circle(arrowEnd, 0.3F, t.color);

         // ESP marker above head with estimated damage
         if (this.showDamageEst.get()) {
            class_243 head = t.player.method_73189().method_1031(0.0, (double)t.player.method_17682() + 0.5, 0.0);
            Render3D.drawFilled(
               new class_238(head.field_1352 - 0.3, head.field_1351, head.field_1350 - 0.1,
                             head.field_1352 + 0.3, head.field_1351 + 0.4, head.field_1350 + 0.1),
               t.color | 0xCC000000
            );
         }
      }
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList();
      if (this.threats.isEmpty()) {
         lines.add("§7—");
         return RenderUtil.panel(ctx, x, y, "Mace Alert", lines);
      }

      for (Threat t : this.threats) {
         String color = switch (t.level) {
            case "IMPACT" -> "§c";
            case "DANGER" -> "§6";
            default -> "§e";
         };
         String name = t.player.method_5477().getString();
         lines.add(color + "§l" + t.level + " §r" + color + name);

         // Timer
         if (this.showTimer.get() && t.timeToImpact > 0 && t.timeToImpact < 100) {
            lines.add("§7  ⏱ " + t.timeToImpact + "ticks → impact");
         }
         if (this.showDamageEst.get()) {
            lines.add("§7  ⚔ ~" + String.format(Locale.ROOT, "%.0f", t.estimatedDmg) + " dmg");
         }
      }

      // Show radar indicator in HUD when sound is active
      if (soundEnabled.get() && this.soundCooldown > 0 && !this.lastLevel.isEmpty()) {
         String levelColor = switch (this.lastLevel) {
            case "IMPACT" -> "§c";
            case "DANGER" -> "§6";
            default -> "§e";
         };
         lines.add("§7  🔊 " + levelColor + "♫ " + this.lastLevel);
      }

      // Screen border flash effect for highest threat level
      String highest = "WATCH";
      for (Threat t : this.threats) {
         if (t.level.equals("IMPACT")) {
            highest = "IMPACT";
            break;
         } else if (t.level.equals("DANGER")) {
            highest = "DANGER";
         }
      }

      return RenderUtil.panel(ctx, x, y, "Mace Alert [" + highest + "]", lines);
   }

   // Record for tracking threats
   private static class Threat {
      final class_742 player;
      final String level;
      final int color;
      final double estimatedDmg;
      final long timeToImpact;
      final class_243 velocity;
      final class_243 pos;

      Threat(class_742 player, String level, int color, double dmg, long tti, class_243 vel, class_243 pos) {
         this.player = player;
         this.level = level;
         this.color = color;
         this.estimatedDmg = dmg;
         this.timeToImpact = tti;
         this.velocity = vel;
         this.pos = pos;
      }
   }
}
