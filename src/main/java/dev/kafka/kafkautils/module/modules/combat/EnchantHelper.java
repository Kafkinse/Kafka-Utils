package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import dev.kafka.kafkautils.util.RenderUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_1890;
import net.minecraft.class_6880;
import net.minecraft.class_7923;
import net.minecraft.class_9304;

/**
 * Enchanting advisor. Reads the tool held in the main hand plus every enchanted
 * book in the inventory and prints the cheapest anvil-combine order to chat —
 * the same result as the offline enchant-order calculator, computed locally with
 * an exact branch-and-bound (no network, no automation). Trigger it by toggling
 * the module on or with {@code /kafka enchant}.
 *
 * <p>Cost model (verified against the game): for a combine of target + sacrifice,
 * {@code cost = (2^tWork-1) + (2^sWork-1) + Σ max(1, anvilCost/2)·level} over the
 * sacrifice's enchantments; result work = {@code max(tWork, sWork)+1}. Inputs are
 * assumed fresh (zero prior work), as a calculator does.
 */
public class EnchantHelper extends Module {
   private static final int MAX_ITEMS = 10; // guard against factorial blow-up
   private static final int INF = 1_000_000;

   private final NumberSetting maxCost = this.add(new NumberSetting("Max Cost/Step", 39, 1, 40, 1));

   private final Map<String, Integer> anvilCost = new HashMap<>();
   private final Map<String, Integer> maxLevel = new HashMap<>();
   private final Map<String, Integer> memo = new HashMap<>();
   private List<Step> plan;

   public EnchantHelper() {
      super("Enchant Helper", "Подсказывает самый дешёвый порядок соединения на наковальне (/kafka enchant).", Category.FARMING);
   }

   protected void onEnable() {
      this.printHints();
   }

   /** Computes and prints the cheapest combine order for the held tool + books. */
   public void printHints() {
      if (mc.field_1724 == null) {
         return;
      }
      class_1799 held = mc.field_1724.method_6047();
      if (!isTool(held)) {
         ChatUtil.info("§d[Зачарование] §7возьми зачаровываемый предмет в руку.");
         return;
      }

      this.anvilCost.clear();
      this.maxLevel.clear();
      this.memo.clear();

      List<Node> nodes = new ArrayList<>();
      String toolName = held.method_7964().getString();
      nodes.add(new Node(true, 0, this.readEnchants(held)));
      for (int i = 0; i < 36; ++i) {
         class_1799 stack = mc.field_1724.method_31548().method_5438(i);
         if (isBook(stack)) {
            Map<String, Integer> ench = this.readEnchants(stack);
            if (!ench.isEmpty()) {
               nodes.add(new Node(false, 0, ench));
            }
         }
      }
      class_1799 off = mc.field_1724.method_6079();
      if (isBook(off)) {
         Map<String, Integer> ench = this.readEnchants(off);
         if (!ench.isEmpty()) {
            nodes.add(new Node(false, 0, ench));
         }
      }

      if (nodes.size() < 2) {
         ChatUtil.info("§d[Зачарование] §7положи книги зачарований в инвентарь.");
         return;
      }
      if (nodes.size() > MAX_ITEMS) {
         ChatUtil.info("§d[Зачарование] §7слишком много книг (>" + MAX_ITEMS + "), убери лишние.");
         return;
      }

      int best = this.solve(nodes);
      if (best >= INF) {
         ChatUtil.info("§d[Зачарование] §7какой-то шаг дороже §c" + this.maxCost.get() + " ур.§7 — возьми свежий предмет и чистые книги (по 1 чару).");
         return;
      }
      this.plan = new ArrayList<>();
      this.reconstruct(nodes);

      ChatUtil.info("§d§l— Порядок зачарования —");
      ChatUtil.info("§7Предмет: §r" + toolName + " §7| Соединений: §r" + this.plan.size());
      int n = 1;
      for (Step s : this.plan) {
         String target = s.targetSword ? "§b" + toolName + label(s.targetEnch) : "§dкнига" + label(s.targetEnch);
         String sac = "§dкнига" + label(s.sacEnch);
         ChatUtil.info("§7" + n + ". §r" + target + " §7+ " + sac + " §7— §a" + s.cost + " ур.");
         ++n;
      }
      ChatUtil.info("§7Итого: §a" + best + " ур. §7(держи столько уровней перед началом)");
   }

   // --- optimizer ---------------------------------------------------------

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
               int step = this.stepCost(o[0], o[1]);
               if (step > this.maxCost.get()) {
                  continue;
               }
               List<Node> next = without(nodes, i, j);
               next.add(this.combine(o[0], o[1]));
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

   private void reconstruct(List<Node> nodes) {
      if (nodes.size() == 1) {
         return;
      }
      int best = this.solve(nodes);
      for (int i = 0; i < nodes.size(); ++i) {
         for (int j = i + 1; j < nodes.size(); ++j) {
            for (Node[] o : orientations(nodes.get(i), nodes.get(j))) {
               int step = this.stepCost(o[0], o[1]);
               if (step > this.maxCost.get()) {
                  continue;
               }
               List<Node> next = without(nodes, i, j);
               next.add(this.combine(o[0], o[1]));
               if (step + this.solve(next) == best) {
                  this.plan.add(new Step(o[0].sword, new HashMap<>(o[0].ench), new HashMap<>(o[1].ench), step));
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

   // --- reading / formatting ---------------------------------------------

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

   /** "(Sharpness V, Mending)" — empty enchant maps render as "". */
   private static String label(Map<String, Integer> ench) {
      if (ench.isEmpty()) {
         return "";
      }
      List<String> parts = new ArrayList<>(ench.size());
      for (Map.Entry<String, Integer> e : ench.entrySet()) {
         parts.add(e.getKey() + (e.getValue() > 1 ? " " + RenderUtil.roman(e.getValue()) : ""));
      }
      Collections.sort(parts);
      return " §7(" + String.join(", ", parts) + "§7)§r";
   }

   private static boolean isBook(class_1799 stack) {
      return !stack.method_7960() && itemId(stack).equals("enchanted_book");
   }

   private static boolean isTool(class_1799 stack) {
      return !stack.method_7960() && !isBook(stack) && (stack.method_7923() || stack.method_7942());
   }

   private static String itemId(class_1799 stack) {
      return class_7923.field_41178.method_10221(stack.method_7909()).method_12832();
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

      String fullSig() {
         List<String> parts = new ArrayList<>(this.ench.size());
         for (Map.Entry<String, Integer> e : this.ench.entrySet()) {
            parts.add(e.getKey() + ":" + e.getValue());
         }
         Collections.sort(parts);
         return (this.sword ? "T" : "B") + this.work + "|" + String.join(",", parts);
      }
   }

   /** One printed instruction: combine target + sacrifice for {@code cost} levels. */
   private static final class Step {
      final boolean targetSword;
      final Map<String, Integer> targetEnch;
      final Map<String, Integer> sacEnch;
      final int cost;

      Step(boolean targetSword, Map<String, Integer> targetEnch, Map<String, Integer> sacEnch, int cost) {
         this.targetSword = targetSword;
         this.targetEnch = targetEnch;
         this.sacEnch = sacEnch;
         this.cost = cost;
      }
   }
}
