package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.class_1304;
import net.minecraft.class_1799;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;

/**
 * Warns when your own gear (armour, main-hand and off-hand items) is close to
 * breaking, both as a HUD panel and (optionally) a one-off chat + sound alert
 * per item as it crosses the threshold.
 */
public class SelfDurabilityAlert extends Module implements HudModule {
   private final NumberSetting threshold = this.add(new NumberSetting("Threshold %", 10, 1, 100, 5));
   private final BooleanSetting chatAlert = this.add(new BooleanSetting("Chat Alert", true));
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));

   private final List<String> low = new ArrayList<>();
   private final Set<String> warned = new HashSet<>();

   public SelfDurabilityAlert() {
      super("Self Durability", "Warns when your own armour/tools are about to break.", Category.COMBAT);
   }

   protected void onEnable() {
      this.low.clear();
      this.warned.clear();
   }

   public void onTick() {
      if (mc.field_1724 == null) {
         return;
      }
      this.low.clear();
      Set<String> stillLow = new HashSet<>();

      for (class_1304 slot : class_1304.values()) {
         if (slot.method_46643()) {
            this.check(mc.field_1724.method_6118(slot), stillLow);
         }
      }
      this.check(mc.field_1724.method_6047(), stillLow);
      this.check(mc.field_1724.method_6079(), stillLow);

      this.warned.retainAll(stillLow);
   }

   private void check(class_1799 stack, Set<String> stillLow) {
      if (stack.method_7960() || !stack.method_7963()) {
         return;
      }
      int max = stack.method_7936();
      if (max <= 0) {
         return;
      }
      int left = max - stack.method_7919();
      int pct = Math.round((float)left / (float)max * 100.0F);
      if (pct <= this.threshold.get()) {
         String name = stack.method_7964().getString();
         this.low.add("§c" + name + " §7— " + pct + "%");
         stillLow.add(name);
         if (!this.warned.contains(name)) {
            this.warned.add(name);
            if (this.chatAlert.get()) {
               ChatUtil.info("§cПрочность §r" + name + " §c" + pct + "%");
            }
            if (this.sound.get() && mc.field_1724 != null) {
               mc.field_1724.method_5783((class_3414)class_3417.field_14622.comp_349(), 1.0F, 0.7F);
            }
         }
      }
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList<>(this.low);
      if (lines.isEmpty()) {
         lines.add("§aвсё цело");
      }
      return RenderUtil.panel(ctx, x, y, "Durability (≤" + this.threshold.get() + "%)", lines);
   }
}
