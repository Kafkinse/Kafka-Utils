package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_1268;
import net.minecraft.class_1703;
import net.minecraft.class_1706;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_1890;
import net.minecraft.class_239;
import net.minecraft.class_2680;
import net.minecraft.class_3965;
import net.minecraft.class_7923;

/**
 * Full-auto anvil combiner. When an anvil is open (or auto-opened by looking at
 * one) it merges all enchanted books onto the target tool in a near-cheapest
 * order and takes the result.
 *
 * <p>Order strategy: the order-dependent part of anvil XP is dominated by the
 * "prior work" penalty (2^k − 1), which is minimised by a balanced merge tree.
 * We approximate that greedily — always combine the two books with the fewest
 * enchantments first (balancing the tree), then apply the merged book to the
 * tool last. Every real step is gated on the anvil's reported level cost, so we
 * never overspend or hit "Too Expensive!".
 *
 * <p>NOTE: GUI-driven automation cannot be exercised in the build environment;
 * slot indices and timings assume the vanilla anvil layout (slots 0/1 = inputs,
 * 2 = output, 3–38 = player inventory) and may need tuning against a live server.
 */
public class AutoEnchant extends Module {
   private static final class_1268 MAIN_HAND = class_1268.values()[0];
   private static final class_1713 PICKUP = class_1713.field_7790;
   private static final class_1713 QUICK_MOVE = class_1713.field_7794;
   private static final int SLOT_INPUT_A = 0;
   private static final int SLOT_INPUT_B = 1;
   private static final int SLOT_OUTPUT = 2;
   private static final int PLAYER_SLOT_START = 3;
   private static final int PLAYER_SLOT_END = 38; // inclusive

   private final BooleanSetting autoOpen = this.add(new BooleanSetting("Auto-Open Anvil", true));
   private final NumberSetting maxCost = this.add(new NumberSetting("Max Cost/Step", 39, 1, 40, 1));
   private final NumberSetting delay = this.add(new NumberSetting("Action Delay", 3, 1, 20, 1));
   private final StringSetting targetFilter = this.add(new StringSetting("Target Filter", ""));

   private int phase;
   private int timer;
   private int combines;
   private int totalCost;
   private boolean reported;
   private int openCooldown;

   public AutoEnchant() {
      super("Auto Enchant", "Combines enchanted books onto a tool on the anvil, cheapest-first.", Category.FARMING);
   }

   protected void onEnable() {
      this.phase = 0;
      this.timer = 0;
      this.combines = 0;
      this.totalCost = 0;
      this.reported = false;
      this.openCooldown = 0;
   }

   public boolean isBusy() {
      return this.isAnvilOpen();
   }

   public void onTick() {
      if (mc.field_1724 == null || mc.field_1761 == null) {
         return;
      }
      if (this.openCooldown > 0) {
         --this.openCooldown;
      }

      if (!this.isAnvilOpen()) {
         if (this.autoOpen.get() && mc.field_1755 == null && this.openCooldown == 0) {
            this.tryOpenAnvil();
         }
         return;
      }

      if (this.timer > 0) {
         --this.timer;
         return;
      }

      switch (this.phase) {
         case 0 -> this.stepPlace();
         case 1 -> this.phase = 2; // waited a cycle for updateResult
         case 2 -> this.stepTake();
      }
   }

   /** Places the next pair into the input slots (clearing the anvil first). */
   private void stepPlace() {
      class_1703 h = mc.field_1724.field_7512;
      if (!this.slotEmpty(h, SLOT_INPUT_A) || !this.slotEmpty(h, SLOT_INPUT_B) || !this.slotEmpty(h, SLOT_OUTPUT)) {
         this.clearAnvil(h);
         this.timer = this.delay.get();
         return;
      }

      int[] pair = this.pickPair(h);
      if (pair == null) {
         this.finish();
         return;
      }

      this.click(pair[0], 0, PICKUP);          // grab target
      this.click(SLOT_INPUT_A, 0, PICKUP);     // place into left input
      this.click(pair[1], 0, PICKUP);          // grab sacrifice
      this.click(SLOT_INPUT_B, 0, PICKUP);     // place into right input
      this.phase = 1;
      this.timer = this.delay.get();
   }

   /** Reads the anvil's real cost and either takes the result or aborts safely. */
   private void stepTake() {
      class_1706 anvil = (class_1706)mc.field_1724.field_7512;
      class_1799 output = anvil.method_7611(SLOT_OUTPUT).method_7677();
      int cost = anvil.method_17369();

      if (output.method_7960()) {
         this.abort("несовместимые предметы");
         return;
      }
      if (cost > this.maxCost.get()) {
         this.abort("слишком дорого: " + cost + " ур.");
         return;
      }
      if (cost > mc.field_1724.field_7520) {
         this.abort("не хватает опыта: нужно " + cost + " ур.");
         return;
      }

      this.click(SLOT_OUTPUT, 0, QUICK_MOVE);  // take result back to inventory
      ++this.combines;
      this.totalCost += cost;
      this.reported = false;
      this.phase = 0;
      this.timer = this.delay.get();
   }

   /** Chooses the two items to combine next: books balanced first, tool last. */
   private int[] pickPair(class_1703 h) {
      List<int[]> books = new ArrayList<>(); // {slot, enchantCount}
      int toolSlot = -1;
      for (int i = PLAYER_SLOT_START; i <= PLAYER_SLOT_END; ++i) {
         class_1799 stack = h.method_7611(i).method_7677();
         if (stack.method_7960()) {
            continue;
         }
         if (this.isBook(stack)) {
            int n = enchantCount(stack);
            if (n > 0) {
               books.add(new int[]{i, n});
            }
         } else if (toolSlot < 0 && this.isTool(stack)) {
            toolSlot = i;
         }
      }

      if (books.size() >= 2) {
         // two books with the fewest enchantments (keeps the merge tree balanced)
         books.sort((x, y) -> Integer.compare(x[1], y[1]));
         return new int[]{books.get(1)[0], books.get(0)[0]}; // target = more, sacrifice = fewer
      }
      if (books.size() == 1 && toolSlot >= 0) {
         return new int[]{toolSlot, books.get(0)[0]};
      }
      return null;
   }

   private void clearAnvil(class_1703 h) {
      for (int s : new int[]{SLOT_OUTPUT, SLOT_INPUT_A, SLOT_INPUT_B}) {
         if (!this.slotEmpty(h, s)) {
            this.click(s, 0, QUICK_MOVE);
         }
      }
   }

   private void abort(String reason) {
      class_1703 h = mc.field_1724.field_7512;
      this.clearAnvil(h);
      ChatUtil.info("§d[Auto Enchant] §7стоп: §r" + reason);
      this.phase = 0;
      this.timer = this.delay.get();
      this.setEnabled(false);
   }

   private void finish() {
      if (!this.reported && this.combines > 0) {
         ChatUtil.info("§d[Auto Enchant] §7готово. Соединений: §r" + this.combines + " §7Итого опыта: §r" + this.totalCost + " ур.");
      }
      this.reported = true;
      this.timer = 20; // idle poll ~1s in case more books show up
   }

   private void tryOpenAnvil() {
      class_239 crosshair = mc.field_1765;
      if (crosshair instanceof class_3965 hit) {
         class_2680 state = mc.field_1687 == null ? null : mc.field_1687.method_8320(hit.method_17777());
         if (state != null && this.blockId(state).contains("anvil")) {
            mc.field_1761.method_2896(mc.field_1724, MAIN_HAND, hit);
            this.openCooldown = 20;
         }
      }
   }

   // --- helpers -----------------------------------------------------------

   private boolean isAnvilOpen() {
      return mc.field_1724 != null && mc.field_1724.field_7512 instanceof class_1706;
   }

   private boolean slotEmpty(class_1703 h, int index) {
      return h.method_7611(index).method_7677().method_7960();
   }

   private void click(int slot, int button, class_1713 type) {
      mc.field_1761.method_2906(mc.field_1724.field_7512.field_7763, slot, button, type, mc.field_1724);
   }

   private boolean isBook(class_1799 stack) {
      return itemId(stack).equals("enchanted_book");
   }

   private boolean isTool(class_1799 stack) {
      if (!stack.method_7923() && !stack.method_7942()) {
         return false;
      }
      String filter = this.targetFilter.get().trim().toLowerCase(Locale.ROOT);
      return filter.isEmpty() || itemId(stack).contains(filter);
   }

   private static int enchantCount(class_1799 stack) {
      return class_1890.method_57532(stack).method_57539().size();
   }

   private static String itemId(class_1799 stack) {
      return class_7923.field_41178.method_10221(stack.method_7909()).method_12832();
   }

   private String blockId(class_2680 state) {
      return class_7923.field_41175.method_10221(state.method_26204()).method_12832();
   }
}
