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
import net.minecraft.class_1890;
import net.minecraft.class_2302;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_3965;
import net.minecraft.class_7923;

/**
 * Harvests fully grown crops around the player and replants a seed from the
 * hotbar. Works on vanilla {@link class_2302 CropBlock} crops (wheat, carrots,
 * potatoes, beetroot).
 *
 * <p>Two things are configurable per the request: <b>Rotations</b> can be turned
 * off so the view never snaps to the crop (breaks are sent by packet only), and
 * <b>Auto Tool</b> switches to a matching harvest tool — e.g. a Fortune hoe — so
 * drops are multiplied, restoring the previous slot afterwards. Yields to Auto
 * Eat / Auto Repair via {@link #isBusy}.
 */
public class AutoReplant extends Module {
   private static final class_1268 MAIN_HAND = class_1268.values()[0];
   private static final class_2350 UP = class_2350.field_11036;

   private final BooleanSetting replant = this.add(new BooleanSetting("Replant", true));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotations", true));
   private final BooleanSetting useTool = this.add(new BooleanSetting("Auto Tool (Fortune)", true));
   private final StringSetting toolKeyword = this.add(new StringSetting("Harvest Tool", "hoe"));
   private final NumberSetting range = this.add(new NumberSetting("Range", 4, 1, 6, 1));
   private final NumberSetting speed = this.add(new NumberSetting("Speed", 2, 1, 20, 1));
   private final BooleanSetting noSwing = this.add(new BooleanSetting("No Swing", false));
   private final StringSetting crops = this.add(new StringSetting("Crops", "wheat,carrot,potato,beetroot"));

   private int tickCounter;
   private int delayTicks;
   private class_2338 plantPos;
   private String plantSeed = "";
   private int savedSlot = -1;
   private int harvested;

   public AutoReplant() {
      super("Auto Replant", "Harvests mature crops around you (optional Fortune tool) and replants seeds.", Category.FARMING);
   }

   protected void onEnable() {
      this.tickCounter = 0;
      this.delayTicks = 0;
      this.plantPos = null;
      this.plantSeed = "";
      this.savedSlot = -1;
      this.harvested = 0;
   }

   protected void onDisable() {
      this.restoreSlot();
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
         this.restoreSlot();
         return;
      }

      class_2338 crop = this.findCrop();
      if (crop == null) {
         this.restoreSlot();
         return;
      }

      if (this.useTool.get()) {
         int tool = this.findTool();
         if (tool >= 0) {
            this.selectSlot(tool);
         }
      }
      if (this.rotate.get()) {
         this.lookAt(crop);
      }

      String cropId = this.blockId(mc.field_1687.method_8320(crop));
      boolean broken = mc.field_1761.method_2902(crop, UP);
      if (!this.noSwing.get()) {
         mc.field_1724.method_6104(MAIN_HAND);
      }
      if (broken) {
         ++this.harvested;
         if (this.replant.get()) {
            this.plantPos = crop;
            this.plantSeed = seedFor(cropId);
            this.delayTicks = 1; // let the block clear before replanting
         } else {
            this.restoreSlot();
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
      this.selectSlot(seedSlot);
      if (this.rotate.get()) {
         this.lookAt(this.plantPos);
      }
      class_2338 farmland = new class_2338(this.plantPos.method_10263(), this.plantPos.method_10264() - 1, this.plantPos.method_10260());
      class_243 vec = new class_243(this.plantPos.method_10263() + 0.5, this.plantPos.method_10264(), this.plantPos.method_10260() + 0.5);
      class_3965 hit = new class_3965(vec, UP, farmland, false);
      mc.field_1761.method_2896(mc.field_1724, MAIN_HAND, hit);
      if (!this.noSwing.get()) {
         mc.field_1724.method_6104(MAIN_HAND);
      }
   }

   /** Nearest mature target crop within reach, or null. */
   private class_2338 findCrop() {
      int r = this.range.get();
      int px = (int)Math.floor(mc.field_1724.method_23317());
      int py = (int)Math.floor(mc.field_1724.method_23318());
      int pz = (int)Math.floor(mc.field_1724.method_23321());
      class_2338 best = null;
      double bestDist = Double.MAX_VALUE;
      for (int dx = -r; dx <= r; ++dx) {
         for (int dz = -r; dz <= r; ++dz) {
            for (int dy = -2; dy <= 2; ++dy) {
               class_2338 pos = new class_2338(px + dx, py + dy, pz + dz);
               class_2680 state = mc.field_1687.method_8320(pos);
               if (state.method_26204() instanceof class_2302 c && c.method_9825(state) && this.isTargetCrop(state)) {
                  double d = pos.method_10268(mc.field_1724.method_23317(), mc.field_1724.method_23318(), mc.field_1724.method_23321());
                  if (d < bestDist && d <= (double)(r * r)) {
                     bestDist = d;
                     best = pos;
                  }
               }
            }
         }
      }
      return best;
   }

   /** Turns the player's view toward the block centre (visible rotation). */
   private void lookAt(class_2338 pos) {
      double dx = pos.method_10263() + 0.5 - mc.field_1724.method_23317();
      double dy = pos.method_10264() + 0.5 - mc.field_1724.method_23320();
      double dz = pos.method_10260() + 0.5 - mc.field_1724.method_23321();
      double horiz = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
      float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horiz)));
      mc.field_1724.method_36456(yaw);
      mc.field_1724.method_36457(pitch);
   }

   private void selectSlot(int slot) {
      if (this.savedSlot < 0) {
         this.savedSlot = mc.field_1724.method_31548().method_67532();
      }
      mc.field_1724.method_31548().method_61496(slot);
   }

   private void restoreSlot() {
      if (this.savedSlot >= 0 && mc.field_1724 != null) {
         mc.field_1724.method_31548().method_61496(this.savedSlot);
         this.savedSlot = -1;
      }
   }

   /** Hotbar slot of the harvest tool (matching keyword), preferring the most enchanted (Fortune). */
   private int findTool() {
      int best = -1;
      int bestEnch = -1;
      for (int i = 0; i < 9; ++i) {
         class_1799 stack = mc.field_1724.method_31548().method_5438(i);
         if (!stack.method_7960() && itemId(stack).contains(this.toolKeyword.get().trim().toLowerCase(Locale.ROOT))) {
            int e = class_1890.method_57532(stack).method_57539().size();
            if (e > bestEnch) {
               bestEnch = e;
               best = i;
            }
         }
      }
      return best;
   }

   private boolean isTargetCrop(class_2680 state) {
      String id = this.blockId(state);
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
         if (!stack.method_7960() && itemId(stack).contains(keyword)) {
            return i;
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

   private static String itemId(class_1799 stack) {
      return class_7923.field_41178.method_10221(stack.method_7909()).method_12832();
   }

   private String blockId(class_2680 state) {
      return class_7923.field_41175.method_10221(state.method_26204()).method_12832();
   }
}
