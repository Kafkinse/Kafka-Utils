package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import java.util.Locale;
import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_2302;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_3965;
import net.minecraft.class_7923;

/**
 * Harvests fully grown crops the player is looking at and replants a seed from
 * the hotbar. Works on vanilla {@link class_2302 CropBlock} crops (wheat,
 * carrots, potatoes, beetroot). Drives the interaction manager (break + place)
 * like Auto Mine and yields to Auto Eat / Auto Repair via {@link #isBusy}.
 */
public class AutoReplant extends Module {
   private static final class_1268 MAIN_HAND = class_1268.values()[0];

   private final BooleanSetting replant = this.add(new BooleanSetting("Replant", true));
   private final BooleanSetting noSwing = this.add(new BooleanSetting("No Swing", false));
   private final NumberSetting speed = this.add(new NumberSetting("Speed", 2, 1, 20, 1));
   private final StringSetting crops = this.add(new StringSetting("Crops", "wheat,carrot,potato,beetroot"));

   private int tickCounter;
   private int delayTicks;
   private class_2338 plantPos;
   private String plantSeed = "";
   private int harvested;

   public AutoReplant() {
      super("Auto Replant", "Harvests mature crops you look at and replants seeds.", Category.FARMING);
   }

   protected void onEnable() {
      this.tickCounter = 0;
      this.delayTicks = 0;
      this.plantPos = null;
      this.plantSeed = "";
      this.harvested = 0;
   }

   public void onTick() {
      if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null || mc.field_1755 != null) {
         return;
      }
      // Yield to Auto Eat / Auto Repair so they don't fight over the held item.
      AutoEat eat = ModuleManager.get(AutoEat.class);
      AutoRepair repair = ModuleManager.get(AutoRepair.class);
      if ((eat != null && eat.isBusy()) || (repair != null && repair.isBusy())) {
         return;
      }
      if (this.delayTicks > 0) {
         --this.delayTicks;
         return;
      }
      if (++this.tickCounter < this.speed.get()) {
         return;
      }
      this.tickCounter = 0;

      // Second step of a harvest: replant into the farmland we just cleared.
      if (this.plantPos != null) {
         this.doReplant();
         this.plantPos = null;
         return;
      }

      class_239 crosshair = mc.field_1765;
      if (!(crosshair instanceof class_3965 hit)) {
         return;
      }
      class_2338 pos = hit.method_17777();
      class_2680 state = mc.field_1687.method_8320(pos);
      if (state.method_26204() instanceof class_2302 crop && crop.method_9825(state) && this.isTargetCrop(pos)) {
         boolean broken = mc.field_1761.method_2902(pos, hit.method_17780());
         if (!this.noSwing.get()) {
            mc.field_1724.method_6104(MAIN_HAND);
         }
         if (broken) {
            ++this.harvested;
            if (this.replant.get()) {
               this.plantPos = pos;
               this.plantSeed = seedFor(this.blockId(state));
               this.delayTicks = 1; // let the block clear before replanting
            }
         }
      }
   }

   /** Right-clicks the farmland under the harvested crop with a matching seed. */
   private void doReplant() {
      if (this.plantSeed.isEmpty()) {
         return;
      }
      int seedSlot = this.findSeed(this.plantSeed);
      if (seedSlot < 0) {
         return;
      }
      int saved = mc.field_1724.method_31548().method_67532();
      mc.field_1724.method_31548().method_61496(seedSlot);

      class_2338 farmland = this.plantPos.method_10074();
      class_243 vec = new class_243(this.plantPos.method_10263() + 0.5, this.plantPos.method_10264(), this.plantPos.method_10260() + 0.5);
      class_3965 hit = new class_3965(vec, class_2350.field_11036, farmland, false);
      mc.field_1761.method_2896(mc.field_1724, MAIN_HAND, hit);
      if (!this.noSwing.get()) {
         mc.field_1724.method_6104(MAIN_HAND);
      }

      mc.field_1724.method_31548().method_61496(saved);
   }

   private boolean isTargetCrop(class_2338 pos) {
      String id = this.blockId(mc.field_1687.method_8320(pos));
      for (String t : this.crops.get().split("[,\\s]+")) {
         if (!t.isBlank() && id.contains(t.trim().toLowerCase(Locale.ROOT))) {
            return true;
         }
      }
      return false;
   }

   private int findSeed(String keyword) {
      for (int i = 0; i < 9; ++i) {
         class_1799 stack = mc.field_1724.method_31548().method_5438(i);
         if (!stack.method_7960()) {
            String id = class_7923.field_41178.method_10221(stack.method_7909()).method_12832();
            if (id.contains(keyword)) {
               return i;
            }
         }
      }
      return -1;
   }

   /** Registry-id keyword of the seed item that replants a given crop block. */
   private static String seedFor(String cropId) {
      if (cropId.contains("wheat")) {
         return "wheat_seeds";
      } else if (cropId.contains("beetroot")) {
         return "beetroot_seeds";
      } else if (cropId.contains("carrot")) {
         return "carrot";
      } else if (cropId.contains("potato")) {
         return "potato";
      }
      return "";
   }

   private String blockId(class_2680 state) {
      return class_7923.field_41175.method_10221(state.method_26204()).method_12832();
   }
}
