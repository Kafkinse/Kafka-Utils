package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.class_1268;
import net.minecraft.class_1703;
import net.minecraft.class_1706;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_1890;
import net.minecraft.class_239;
import net.minecraft.class_2680;
import net.minecraft.class_3965;
import net.minecraft.class_6880;
import net.minecraft.class_7923;
import net.minecraft.class_9304;

/**
 * Full-auto anvil combiner that minimises total XP the same way as an offline
 * anvil calculator: an exact branch-and-bound over every item (the tool plus all
 * enchanted books), not a greedy "books first" heuristic. Each enchantment is
 * re-charged at every merge level it passes through as a sacrifice, so the true
 * optimum interleaves applying books to the tool with merging books together.
 *
 * <p>Cost model (per combine of target + sacrifice), verified against the game:
 * {@code cost = (2^targetWork-1) + (2^sacrificeWork-1) + Σ max(1, anvilCost/2)·level}
 * over the sacrifice's enchantments; the result's work count is
 * {@code max(targetWork, sacrificeWork) + 1}. Input items are assumed to start
 * with zero prior work (as a calculator does); every executed step is still
 * gated on the anvil's real reported cost, so it never overspends.
 *
 * <p>NOTE: GUI automation cannot be exercised in the build environment; slot
 * indices and timings assume the vanilla anvil layout and may need tuning.
 */
public class AutoEnchant extends Module {
   private static final class_1268 MAIN_HAND = class_1268.values()[0];
   private static final class_1713 PICKUP = class_1713.field_7790;
   private static final class_1713 QUICK_MOVE = class_1713.field_7794;
   private static final int SLOT_INPUT_A = 0;
   private static final int SLOT_INPUT_B = 1;
   private static final int SLOT_OUTPUT = 2;
   private static final int PLAYER_SLOT_START = 3;
   private static final int PLAYER_SLOT_END = 38; // inclusive, vanilla anvil layout
   private static final int MAX_ITEMS = 10;       // guard against factorial blow-up
   private static final int INF = 1_000_000;

   private final BooleanSetting autoOpen = this.add(new BooleanSetting("Auto-Open Anvil", true));
   private final NumberSetting maxCost = this.add(new NumberSetting("Max Cost/Step", 39, 1, 40, 1));
   private final NumberSetting delay = this.add(new NumberSetting("Action Delay", 3, 1, 20, 1));
   private final StringSetting targetFilter = this.add(new StringSetting("Target Filter", "sword"));

   private final Map<String, Integer> anvilCost = new HashMap<>();
   private final Map<String, Integer> maxLevel = new HashMap<>();
   private final Map<String, Integer> memo = new HashMap<>();

   private List<String[]> plan; // each step: {targetSig, sacrificeSig} (enchant-only sigs)
   private int planIndex;
   private int phase;
   private int timer;
   private int combines;
   private int totalCost;
   private int openCooldown;

   public AutoEnchant() {
      super("Auto Enchant", "Combines enchanted books onto a tool on the anvil for the cheapest total XP.", Category.FARMING);
   }

   protected void onEnable() {
      this.reset();
      this.combines = 0;
      this.totalCost = 0;
      this.openCooldown = 0;
   }

   private void reset() {
      this.plan = null;
      this.planIndex = 0;
      this.phase = 0;
      this.timer = 0;
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
         if (this.autoOpen.get() && mc.field_1755 == null && this.openCooldown == 0 && this.hasWork()) {
            this.tryOpenAnvil();
         }
         return;
      }

      if (this.timer > 0) {
         --this.timer;
         return;
      }

      class_1703 h = mc.field_1724.field_7512;
      if (this.plan == null) {
         this.buildPlan(h);
         return;
      }
      if (this.planIndex >= this.plan.size()) {
         this.finish();
         return;
      }

      switch (this.phase) {
         case 0 -> this.stepPlace(h);
         case 1 -> this.phase = 2; // waited a cycle for updateResult
         case 2 -> this.stepTake();
      }
   }

   // --- planning ----------------------------------------------------------

   private void buildPlan(class_1703 h) {
      this.anvilCost.clear();
      this.maxLevel.clear();
      this.memo.clear();

      List<Node> nodes = new ArrayList<>();
      Node sword = null;
      for (int i = PLAYER_SLOT_START; i <= PLAYER_SLOT_END; ++i) {
         class_1799 stack = h.method_7611(i).method_7677();
         if (stack.method_7960()) {
            continue;
         }
         if (this.isBook(stack)) {
            Map<String, Integer> ench = this.readEnchants(stack);
            if (!ench.isEmpty()) {
               nodes.add(new Node(false, 0, ench));
            }
         } else if (sword == null && this.isTool(stack)) {
            sword = new Node(true, 0, this.readEnchants(stack));
         }
      }
      if (sword != null) {
         nodes.add(sword);
      }

      if (nodes.size() < 2 || sword == null) {
         if (this.combines > 0) {
            this.finish();
         } else {
            this.timer = 10; // idle: nothing to combine, don't busy-loop
         }
         return; // leave the anvil for the player
      }
      if (nodes.size() > MAX_ITEMS) {
         this.abort("слишком много предметов (>" + MAX_ITEMS + ")");
         return;
      }

      int best = this.solve(nodes);
      if (best >= INF) {
         this.abort("нельзя уложиться в " + this.maxCost.get() + " ур./шаг");
         return;
      }
      this.plan = new ArrayList<>();
      this.reconstruct(nodes);
      this.planIndex = 0;
      this.phase = 0;
      ChatUtil.info("§d[Auto Enchant] §7план на §r" + this.plan.size() + " §7соединений, расчётно §r" + best + " §7ур.");
   }

   /** Minimum total level cost to merge {@code nodes} into one item (memoised). */
   private int solve(List<Node> nodes) {
      if (nodes.size() == 1) {
         return 0;
      }
      String key = key(nodes);
      Integer cached = this.memo.get(key);
      if (cached != null) {
         return cached;
      }
      int best = INF;
      for (int i = 0; i < nodes.size(); ++i) {
         for (int j = i + 1; j < nodes.size(); ++j) {
            for (Node[] o : orientations(nodes.get(i), nodes.get(j))) {
               int step = stepCost(o[0], o[1]);
               if (step > this.maxCost.get()) {
                  continue;
               }
               List<Node> next = without(nodes, i, j);
               next.add(combine(o[0], o[1]));
               int total = step + this.solve(next);
               if (total < best) {
                  best = total;
               }
            }
         }
      }
      this.memo.put(key, best);
      return best;
   }

   /** Re-walks the optimal tree (using the memo) to emit the ordered steps. */
   private void reconstruct(List<Node> nodes) {
      if (nodes.size() == 1) {
         return;
      }
      int best = this.solve(nodes);
      for (int i = 0; i < nodes.size(); ++i) {
         for (int j = i + 1; j < nodes.size(); ++j) {
            for (Node[] o : orientations(nodes.get(i), nodes.get(j))) {
               int step = stepCost(o[0], o[1]);
               if (step > this.maxCost.get()) {
                  continue;
               }
               List<Node> next = without(nodes, i, j);
               next.add(combine(o[0], o[1]));
               if (step + this.solve(next) == best) {
                  this.plan.add(new String[]{o[0].matchSig(), o[1].matchSig()});
                  this.reconstruct(next);
                  return;
               }
            }
         }
      }
   }

   private int stepCost(Node target, Node sacrifice) {
      int cost = penalty(target.work) + penalty(sacrifice.work);
      for (Map.Entry<String, Integer> e : sacrifice.ench.entrySet()) {
         int ac = this.anvilCost.getOrDefault(e.getKey(), 1);
         cost += Math.max(1, ac / 2) * e.getValue();
      }
      return cost;
   }

   private Node combine(Node target, Node sacrifice) {
      Map<String, Integer> res = new HashMap<>(target.ench);
      for (Map.Entry<String, Integer> e : sacrifice.ench.entrySet()) {
         String name = e.getKey();
         int lvl = e.getValue();
         Integer cur = res.get(name);
         if (cur == null) {
            res.put(name, lvl);
         } else if (cur.intValue() == lvl) {
            res.put(name, Math.min(this.maxLevel.getOrDefault(name, lvl), lvl + 1));
         } else {
            res.put(name, Math.max(cur, lvl));
         }
      }
      return new Node(target.sword, Math.max(target.work, sacrifice.work) + 1, res);
   }

   private static List<Node[]> orientations(Node a, Node b) {
      List<Node[]> out = new ArrayList<>(2);
      if (a.sword) {
         out.add(new Node[]{a, b});
      } else if (b.sword) {
         out.add(new Node[]{b, a});
      } else {
         out.add(new Node[]{a, b});
         out.add(new Node[]{b, a});
      }
      return out;
   }

   private static List<Node> without(List<Node> nodes, int i, int j) {
      List<Node> out = new ArrayList<>(nodes.size() - 1);
      for (int k = 0; k < nodes.size(); ++k) {
         if (k != i && k != j) {
            out.add(nodes.get(k));
         }
      }
      return out;
   }

   private static int penalty(int work) {
      return (1 << work) - 1;
   }

   private static String key(List<Node> nodes) {
      List<String> sigs = new ArrayList<>(nodes.size());
      for (Node n : nodes) {
         sigs.add(n.fullSig());
      }
      Collections.sort(sigs);
      return String.join(";", sigs);
   }

   // --- execution ---------------------------------------------------------

   private void stepPlace(class_1703 h) {
      if (!this.slotEmpty(h, SLOT_INPUT_A) || !this.slotEmpty(h, SLOT_INPUT_B) || !this.slotEmpty(h, SLOT_OUTPUT)) {
         this.clearAnvil(h);
         this.timer = this.delay.get();
         return;
      }
      String[] want = this.plan.get(this.planIndex);
      int targetSlot = this.findSlot(h, want[0], -1);
      int sacrificeSlot = this.findSlot(h, want[1], targetSlot);
      if (targetSlot < 0 || sacrificeSlot < 0) {
         this.abort("не найден предмет для шага " + (this.planIndex + 1));
         return;
      }
      this.click(targetSlot, 0, PICKUP);
      this.click(SLOT_INPUT_A, 0, PICKUP);
      this.click(sacrificeSlot, 0, PICKUP);
      this.click(SLOT_INPUT_B, 0, PICKUP);
      this.phase = 1;
      this.timer = this.delay.get();
   }

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
      this.click(SLOT_OUTPUT, 0, QUICK_MOVE);
      ++this.combines;
      this.totalCost += cost;
      ++this.planIndex;
      this.phase = 0;
      this.timer = this.delay.get();
   }

   /** First player-inventory slot whose enchant-only signature matches, != skip. */
   private int findSlot(class_1703 h, String sig, int skip) {
      for (int i = PLAYER_SLOT_START; i <= PLAYER_SLOT_END; ++i) {
         if (i == skip) {
            continue;
         }
         class_1799 stack = h.method_7611(i).method_7677();
         if (!stack.method_7960() && this.stackSig(stack).equals(sig)) {
            return i;
         }
      }
      return -1;
   }

   private void clearAnvil(class_1703 h) {
      for (int s : new int[]{SLOT_OUTPUT, SLOT_INPUT_A, SLOT_INPUT_B}) {
         if (!this.slotEmpty(h, s)) {
            this.click(s, 0, QUICK_MOVE);
         }
      }
   }

   private void abort(String reason) {
      if (mc.field_1724 != null && mc.field_1724.field_7512 != null) {
         this.clearAnvil(mc.field_1724.field_7512);
      }
      ChatUtil.info("§d[Auto Enchant] §7стоп: §r" + reason);
      this.reset();
      this.setEnabled(false);
   }

   private void finish() {
      if (this.combines > 0) {
         ChatUtil.info("§d[Auto Enchant] §7готово. Соединений: §r" + this.combines + " §7Опыта: §r" + this.totalCost + " ур.");
      }
      if (mc.field_1724 != null) {
         mc.field_1724.method_7346(); // close the anvil so it does not re-open
      }
      this.reset();
      this.setEnabled(false);
   }

   private void tryOpenAnvil() {
      class_239 crosshair = mc.field_1765;
      if (crosshair instanceof class_3965 hit && mc.field_1687 != null) {
         class_2680 state = mc.field_1687.method_8320(hit.method_17777());
         if (this.blockId(state).contains("anvil")) {
            mc.field_1761.method_2896(mc.field_1724, MAIN_HAND, hit);
            this.openCooldown = 20;
         }
      }
   }

   // --- helpers -----------------------------------------------------------

   /** True when the player inventory holds the target tool plus at least one book. */
   private boolean hasWork() {
      boolean tool = false;
      boolean book = false;
      for (int i = 0; i < 36; ++i) {
         class_1799 stack = mc.field_1724.method_31548().method_5438(i);
         if (stack.method_7960()) {
            continue;
         }
         if (this.isBook(stack)) {
            book = true;
         } else if (this.isTool(stack)) {
            tool = true;
         }
      }
      return tool && book;
   }

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

   /** Enchant-only signature of a live stack (matches {@link Node#matchSig}). */
   private String stackSig(class_1799 stack) {
      return sigOf(!this.isBook(stack), this.readEnchants(stack));
   }

   private Map<String, Integer> readEnchants(class_1799 stack) {
      Map<String, Integer> m = new HashMap<>();
      class_9304 comp = class_1890.method_57532(stack);
      for (Object2IntMap.Entry<class_6880<class_1887>> e : comp.method_57539()) {
         class_1887 ench = (class_1887)e.getKey().comp_349();
         String name = ench.comp_2686().getString();
         m.put(name, e.getIntValue());
         this.anvilCost.put(name, ench.method_58446());
         this.maxLevel.put(name, ench.method_8183());
      }
      return m;
   }

   private static String sigOf(boolean sword, Map<String, Integer> ench) {
      List<String> parts = new ArrayList<>(ench.size());
      for (Map.Entry<String, Integer> e : ench.entrySet()) {
         parts.add(e.getKey() + ":" + e.getValue());
      }
      Collections.sort(parts);
      return (sword ? "T|" : "B|") + String.join(",", parts);
   }

   private static String itemId(class_1799 stack) {
      return class_7923.field_41178.method_10221(stack.method_7909()).method_12832();
   }

   private String blockId(class_2680 state) {
      return class_7923.field_41175.method_10221(state.method_26204()).method_12832();
   }

   /** Planning node: an item's enchantments plus its accumulated anvil work count. */
   private static final class Node {
      final boolean sword;
      final int work;
      final Map<String, Integer> ench;

      Node(boolean sword, int work, Map<String, Integer> ench) {
         this.sword = sword;
         this.work = work;
         this.ench = ench;
      }

      String matchSig() {
         return sigOf(this.sword, this.ench);
      }

      String fullSig() {
         return this.matchSig() + "#" + this.work;
      }
   }
}
