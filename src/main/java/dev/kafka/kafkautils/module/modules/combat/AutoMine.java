package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.mixin.ClientPlayerInteractionManagerAccessor;
import dev.kafka.kafkautils.mixin.MinecraftClientAccessor;
import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.ModeSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_239;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2680;
import net.minecraft.class_332;
import net.minecraft.class_3965;
import net.minecraft.class_7923;

/**
 * Auto-mining assistant. In a tight loop it either mines target ores the player
 * is looking at (Mine), places ore from the off-hand and breaks it for Fortune
 * drops (Farm), or does both (Hybrid). It drives the vanilla interaction manager
 * (attack/place/break packets) rather than synthetic input.
 *
 * <p>NOTE: this is packet-level automation that cannot be exercised in the build
 * environment. The core mine/farm loop, auto-swap, HUD and session logger use
 * verified 1.21.11 mappings, but timings and the off-hand refill path may need
 * tuning against a live server. Off-hand refill currently triggers Auto-Stop
 * rather than moving stacks between inventory slots (which needs slot-click
 * packets that should be validated in-game first).
 */
public class AutoMine extends Module implements HudModule {
   private static final class_1268 MAIN_HAND = class_1268.values()[0];
   private static final class_1268 OFF_HAND = class_1268.values()[1];

   private final ModeSetting mode = this.add(new ModeSetting("Mode", 1, "Farm", "Mine", "Hybrid"));
   private final BooleanSetting fastBreak = this.add(new BooleanSetting("Fast Break", true));
   private final BooleanSetting fastPlace = this.add(new BooleanSetting("Fast Place", true));
   private final NumberSetting breakSpeed = this.add(new NumberSetting("Break Speed", 1, 1, 20, 1));
   private final BooleanSetting autoSwap = this.add(new BooleanSetting("Auto Swap Pickaxe", true));
   private final NumberSetting swapThreshold = this.add(new NumberSetting("Swap Threshold %", 10, 1, 100, 5));
   private final BooleanSetting autoRefill = this.add(new BooleanSetting("Auto Refill Offhand", true));
   private final BooleanSetting fortuneOnly = this.add(new BooleanSetting("Fortune Only", false));
   private final BooleanSetting noSwing = this.add(new BooleanSetting("No Swing", false));
   private final NumberSetting antiCheat = this.add(new NumberSetting("AntiCheat Delay", 2, 0, 10, 1));
   private final StringSetting targets = this.add(new StringSetting("Target Ores",
      "diamond,emerald,gold,iron,coal,copper,redstone,lapis,quartz,debris"));

   private final Map<String, Integer> drops = new LinkedHashMap<>();
   private final Random rng = new Random();
   private long sessionStart;
   private int cycles;
   private int brokenPickaxes;
   private int tickCounter;
   private int delayTicks;

   public AutoMine() {
      super("Auto Mine", "Places/breaks ores in a loop for fast Fortune farming.", Category.FARMING);
   }

   protected void onEnable() {
      this.drops.clear();
      this.cycles = 0;
      this.brokenPickaxes = 0;
      this.tickCounter = 0;
      this.delayTicks = 0;
      this.sessionStart = System.currentTimeMillis();
   }

   protected void onDisable() {
      this.printSummary();
   }

   public void onTick() {
      if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) {
         return;
      }
      // Fast Break / Fast Place: clear the vanilla per-action cooldowns each tick.
      if (this.fastBreak.get()) {
         ((ClientPlayerInteractionManagerAccessor)mc.field_1761).setBlockBreakingCooldown(0);
      }
      if (this.fastPlace.get()) {
         ((MinecraftClientAccessor)mc).setItemUseCooldown(0);
      }
      if (this.delayTicks > 0) {
         --this.delayTicks;
         return;
      }
      // Break Speed throttles how often a cycle step runs (ignored under Fast Break).
      if (!this.fastBreak.get() && ++this.tickCounter < this.breakSpeed.get()) {
         return;
      }
      this.tickCounter = 0;

      if (!this.ensurePickaxe()) {
         return;
      }

      class_239 crosshair = mc.field_1765;
      if (!(crosshair instanceof class_3965 hit)) {
         return;
      }
      class_2338 pos = hit.method_17777();
      class_2350 side = hit.method_17780();
      class_2680 state = mc.field_1687.method_8320(pos);

      String modeName = this.mode.get();
      boolean canMine = !modeName.equals("Farm");
      boolean canFarm = !modeName.equals("Mine");
      boolean fortuneOk = !this.fortuneOnly.get() || this.hasFortune(mc.field_1724.method_6047());

      // Desync guard: never keep hitting a block the server already cleared.
      if (canMine && fortuneOk && !state.method_26215() && this.isTargetOre(state)) {
         this.mineBlock(pos, side, state);
         return;
      }

      if (canFarm) {
         class_1799 offhand = mc.field_1724.method_6079();
         if (this.isOre(offhand)) {
            mc.field_1761.method_2896(mc.field_1724, OFF_HAND, hit);
            if (!this.noSwing.get()) {
               mc.field_1724.method_6104(OFF_HAND);
            }
         } else if (this.autoRefill.get()) {
            this.autoStop("руда в offhand закончилась (нужен ин-гейм тест refill)");
         } else {
            this.autoStop("руда в offhand закончилась");
         }
      }
   }

   private void mineBlock(class_2338 pos, class_2350 side, class_2680 state) {
      boolean broken = mc.field_1761.method_2902(pos, side);
      if (!this.noSwing.get()) {
         mc.field_1724.method_6104(MAIN_HAND);
      }
      if (broken) {
         ++this.cycles;
         String type = this.oreType(blockId(state));
         this.drops.merge(type, 1, Integer::sum);
         if (this.antiCheat.get() > 0) {
            this.delayTicks = this.rng.nextInt(this.antiCheat.get() + 1);
         }
      }
   }

   /** @return false and Auto-Stops if there is no usable pickaxe. */
   private boolean ensurePickaxe() {
      class_1799 held = mc.field_1724.method_6047();
      if (this.isPickaxe(held) && this.durabilityPct(held) > this.swapThreshold.get()) {
         return true;
      }
      if (!this.autoSwap.get()) {
         return this.isPickaxe(held);
      }

      // Look for a healthier pickaxe in the hotbar and switch to it.
      int bestSlot = -1;
      int bestPct = this.swapThreshold.get();
      for (int i = 0; i < 9; ++i) {
         class_1799 stack = mc.field_1724.method_31548().method_5438(i);
         if (this.isPickaxe(stack)) {
            int pct = this.durabilityPct(stack);
            if (pct > bestPct) {
               bestPct = pct;
               bestSlot = i;
            }
         }
      }
      if (bestSlot >= 0) {
         if (this.isPickaxe(held) && this.durabilityPct(held) <= this.swapThreshold.get()) {
            ++this.brokenPickaxes;
         }
         mc.field_1724.method_31548().method_61496(bestSlot);
         return true;
      }
      if (!this.isPickaxe(held)) {
         this.autoStop("нет кирки в хотбаре");
         return false;
      }
      return true;
   }

   private void autoStop(String reason) {
      ChatUtil.info("§eAuto Mine остановлен: §r" + reason);
      this.setEnabled(false);
   }

   private void printSummary() {
      if (this.cycles == 0) {
         return;
      }
      double minutes = Math.max(0.001, (System.currentTimeMillis() - this.sessionStart) / 60000.0);
      ChatUtil.info("§d— Auto Mine —");
      ChatUtil.info(String.format(Locale.ROOT, "§7Время: §r%.1f мин  §7Циклы: §r%d", minutes, this.cycles));
      for (Map.Entry<String, Integer> e : this.drops.entrySet()) {
         ChatUtil.info(String.format(Locale.ROOT, "§7%s: §r%d §7(%.1f/мин)",
            e.getKey(), e.getValue(), e.getValue() / minutes));
      }
      ChatUtil.info("§7Сломано кирок: §r" + this.brokenPickaxes);
   }

   // --- helpers -----------------------------------------------------------

   /** Detects a pickaxe by its registry id, so custom-named/enchanted picks still count. */
   private boolean isPickaxe(class_1799 stack) {
      return !stack.method_7960() && itemId(stack).contains("pickaxe");
   }

   /** Best-effort Fortune check: real enchantments or a glint (covers server custom-fortune). */
   private boolean hasFortune(class_1799 stack) {
      return stack.method_7942() || stack.method_7958();
   }

   private int durabilityPct(class_1799 stack) {
      if (!stack.method_7963()) {
         return 100;
      }
      int max = stack.method_7936();
      if (max <= 0) {
         return 100;
      }
      return Math.round((float)(max - stack.method_7919()) / (float)max * 100.0F);
   }

   private boolean isTargetOre(class_2680 state) {
      return matches(blockId(state));
   }

   private boolean isOre(class_1799 stack) {
      return !stack.method_7960() && matches(itemId(stack));
   }

   private boolean matches(String id) {
      for (String t : this.targets.get().split("[,\\s]+")) {
         if (!t.isBlank() && id.contains(t.trim().toLowerCase(Locale.ROOT))) {
            return true;
         }
      }
      return false;
   }

   /** Locale-independent registry path, e.g. "netherite_pickaxe" / "deepslate_diamond_ore". */
   private static String itemId(class_1799 stack) {
      return class_7923.field_41178.method_10221(stack.method_7909()).method_12832();
   }

   private static String blockId(class_2680 state) {
      return class_7923.field_41175.method_10221(state.method_26204()).method_12832();
   }

   private String oreType(String name) {
      for (String t : this.targets.get().split("[,\\s]+")) {
         if (!t.isBlank() && name.contains(t.trim().toLowerCase(Locale.ROOT))) {
            return t.trim();
         }
      }
      return name;
   }

   private static String bar(int pct) {
      int filled = Math.round(pct / 10.0F);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 10; ++i) {
         sb.append(i < filled ? "§a▮" : "§7▯");
      }
      return sb.toString();
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList<>();
      lines.add("§7Режим: §d" + this.mode.get() + " §7| Циклы: §r" + this.cycles);

      if (mc.field_1724 != null) {
         class_1799 pick = mc.field_1724.method_6047();
         if (this.isPickaxe(pick)) {
            int pct = this.durabilityPct(pick);
            lines.add("§7Кирка " + bar(pct) + " §r" + pct + "%");
         }
         class_1799 off = mc.field_1724.method_6079();
         if (this.isOre(off)) {
            lines.add("§7Руда: §r" + off.method_7947() + " шт");
         }
      }

      if (this.drops.isEmpty()) {
         lines.add("§7дропа пока нет");
      } else {
         for (Map.Entry<String, Integer> e : this.drops.entrySet()) {
            lines.add("§d" + e.getKey() + "§7: §r" + e.getValue());
         }
      }
      return RenderUtil.panel(ctx, x, y, "Auto Mine", lines);
   }
}
