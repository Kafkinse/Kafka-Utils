package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import dev.kafka.kafkautils.util.RenderUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_1890;
import net.minecraft.class_6880;
import net.minecraft.class_7923;
import net.minecraft.class_9304;

/**
 * Enchanting advisor. Reads the tool held in the main hand plus enchanted books
 * in the inventory and prints the cheapest anvil-combine order to chat — the
 * same result as the offline enchant-order calculator, computed locally with an
 * exact branch-and-bound (no network, no automation). Trigger by toggling the
 * module on or with {@code /kafka enchant}.
 *
 * <p>You can restrict which enchantments to use with the <b>Wanted</b> setting
 * (like ticking boxes on the website): list enchantment names/ids separated by
 * commas, e.g. {@code density,unbreaking,mending} — leave it empty to use every
 * book found. Mutually-exclusive picks (e.g. Breach + Density on a mace, or
 * Sharpness + Smite on a sword) are detected and reported instead of producing
 * an impossible plan.
 *
 * <p>Cost model (verified against the game): for a combine of target + sacrifice,
 * {@code cost = (2^tWork-1) + (2^sWork-1) + Σ max(1, anvilCost/2)·level}; result
 * work = {@code max(tWork, sWork)+1}. Inputs are assumed fresh (zero prior work).
 */
public class EnchantHelper extends Module {
   private static final int MAX_ITEMS = 10; // guard against factorial blow-up
   private static final int INF = 1_000_000;

   private static final Map<String, String> ENCH_RU = buildNames();
   private static final Map<String, Preset> PRESETS = buildPresets();

   private final NumberSetting maxCost = this.add(new NumberSetting("Max Cost/Step", 39, 1, 40, 1));
   private final StringSetting wanted = this.add(new StringSetting("Wanted", ""));

   private final Map<String, Integer> anvilCost = new HashMap<>();
   private final Map<String, Integer> maxLevel = new HashMap<>();
   private final Map<String, String> display = new HashMap<>();
   private final Map<String, class_6880<class_1887>> entryById = new HashMap<>();
   private final Map<String, Integer> memo = new HashMap<>();
   private List<Step> plan;
   private static boolean openRequested;

   public EnchantHelper() {
      super("Enchant Helper", "Подсказывает самый дешёвый порядок соединения на наковальне (/kafka enchant).", Category.FARMING);
   }

   protected void onEnable() {
      if (mc.field_1724 != null) {
         requestOpen(); // open the enchant menu on the next client tick
      }
   }

   public static void requestOpen() {
      openRequested = true;
   }

   public static boolean consumeOpen() {
      boolean r = openRequested;
      openRequested = false;
      return r;
   }

   /** Prints the cheapest combine order for the held tool + all matching books. */
   public void printHints() {
      for (String line : this.hintLines()) {
         ChatUtil.info(line);
      }
   }

   public List<String> hintLines() {
      List<String> out = new ArrayList<>();
      if (mc.field_1724 == null) {
         return out;
      }
      class_1799 held = mc.field_1724.method_6047();
      if (!isTool(held)) {
         out.add("§d[Зачарование] §7возьми зачаровываемый предмет в руку.");
         return out;
      }
      this.reset();
      List<String> want = this.parseWanted();
      List<Node> nodes = new ArrayList<>();
      String toolName = held.method_7964().getString();
      nodes.add(new Node(true, 0, this.readEnchants(held)));
      int skipped = 0;
      for (int i = 0; i < 36; ++i) {
         skipped += this.addBook(nodes, mc.field_1724.method_31548().method_5438(i), want);
      }
      skipped += this.addBook(nodes, mc.field_1724.method_6079(), want);
      if (nodes.size() < 2) {
         out.add("§d[Зачарование] §7положи подходящие книги зачарований в инвентарь"
            + (want.isEmpty() ? "." : " (по фильтру 'Wanted')."));
         return out;
      }
      out.addAll(this.planLines(nodes, toolName, skipped > 0 ? " §8(книг вне фильтра: " + skipped + ")" : ""));
      return out;
   }

   /** Prints a preset's cheapest order plus which books are still missing. */
   public void printPreset(String key) {
      for (String line : this.presetLines(key)) {
         ChatUtil.info(line);
      }
   }

   public List<String> presetLines(String key) {
      List<String> out = new ArrayList<>();
      if (mc.field_1724 == null) {
         return out;
      }
      String kk = key.toLowerCase(Locale.ROOT);
      Preset p = PRESETS.get(kk);
      if (p == null) {
         out.add("§d[Заготовка] §7неизвестно «" + key + "». Доступно: §r" + String.join(", ", PRESETS.keySet()));
         return out;
      }
      class_1799 held = mc.field_1724.method_6047();
      if (!itemMatches(held, kk)) {
         out.add("§d[Заготовка] §7возьми §r" + p.item + " §7в руку.");
         return out;
      }

      this.reset();
      Map<String, Integer> toolEnch = this.readEnchants(held);
      Map<String, Integer> bestBook = new HashMap<>();
      for (int i = 0; i < 36; ++i) {
         this.collectBook(mc.field_1724.method_31548().method_5438(i), bestBook);
      }
      this.collectBook(mc.field_1724.method_6079(), bestBook);

      List<String> missing = new ArrayList<>();
      for (Map.Entry<String, Integer> e : p.ench.entrySet()) {
         int have = Math.max(toolEnch.getOrDefault(e.getKey(), 0), bestBook.getOrDefault(e.getKey(), 0));
         if (have < e.getValue()) {
            missing.add(this.enchName(e.getKey()) + (e.getValue() > 1 ? " " + RenderUtil.roman(e.getValue()) : ""));
         }
      }
      List<Node> nodes = new ArrayList<>();
      nodes.add(new Node(true, 0, new HashMap<>(toolEnch)));
      for (Map.Entry<String, Integer> e : p.ench.entrySet()) {
         int bookLvl = bestBook.getOrDefault(e.getKey(), 0);
         if (bookLvl > 0 && bookLvl > toolEnch.getOrDefault(e.getKey(), 0)) {
            Map<String, Integer> one = new HashMap<>();
            one.put(e.getKey(), bookLvl);
            nodes.add(new Node(false, 0, one));
         }
      }
      out.add("§d§l— Заготовка: " + p.ru + " —");
      out.add(missing.isEmpty() ? "§aВсе нужные книги есть." : "§eНе хватает книг: §c" + String.join("§7, §c", missing));
      if (nodes.size() < 2) {
         out.add(missing.isEmpty() ? "§aПредмет уже полностью зачарован." : "§7Собери недостающие книги и повтори.");
         return out;
      }
      out.addAll(this.planLines(nodes, held.method_7964().getString(), ""));
      return out;
   }

   private void reset() {
      this.anvilCost.clear();
      this.maxLevel.clear();
      this.display.clear();
      this.entryById.clear();
      this.memo.clear();
   }

   private void collectBook(class_1799 stack, Map<String, Integer> best) {
      if (isBook(stack)) {
         for (Map.Entry<String, Integer> e : this.readEnchants(stack).entrySet()) {
            best.merge(e.getKey(), e.getValue(), Math::max);
         }
      }
   }

   /** Shared: conflict check, optimise, and format the ordered steps as lines. */
   private List<String> planLines(List<Node> nodes, String toolName, String note) {
      List<String> out = new ArrayList<>();
      if (nodes.size() > MAX_ITEMS) {
         out.add("§d[Зачарование] §7слишком много книг (>" + MAX_ITEMS + ").");
         return out;
      }
      String conflict = this.findConflict(nodes);
      if (conflict != null) {
         out.add("§d[Зачарование] §cнесовместимо: §r" + conflict + " §7— оставь одну (укажи нужные в 'Wanted').");
         return out;
      }
      int best = this.solve(nodes);
      if (best >= INF) {
         out.add("§d[Зачарование] §7какой-то шаг дороже §c" + this.maxCost.get() + " ур.§7 — возьми свежий предмет и чистые книги.");
         return out;
      }
      this.plan = new ArrayList<>();
      this.reconstruct(nodes);
      out.add("§d§l— Порядок зачарования —");
      out.add("§7Предмет: §r" + toolName + " §7| Соединений: §r" + this.plan.size() + note);
      int n = 1;
      for (Step s : this.plan) {
         String target = s.targetSword ? "§b" + toolName + this.label(s.targetEnch) : "§dкнига" + this.label(s.targetEnch);
         String sac = "§dкнига" + this.label(s.sacEnch);
         out.add("§7" + n + ". §r" + target + " §7+ " + sac + " §7— §a" + s.cost + " ур.");
         ++n;
      }
      out.add("§7Итого: §a" + best + " ур. §7(держи столько уровней перед началом)");
      return out;
   }

   public List<String> presetKeys() {
      return new ArrayList<>(PRESETS.keySet());
   }

   public String presetTooltip(String key) {
      Preset p = PRESETS.get(key);
      return p == null ? key : p.ru;
   }

   /** Preferred display name: curated RU dictionary, then the game's own name, then id. */
   private String enchName(String id) {
      String ru = ENCH_RU.get(id);
      return ru != null ? ru : this.display.getOrDefault(id, id);
   }

   /** Adds a book node if it passes the Wanted filter; returns 1 if filtered out. */
   private int addBook(List<Node> nodes, class_1799 stack, List<String> want) {
      if (!isBook(stack)) {
         return 0;
      }
      Map<String, Integer> ench = this.readEnchants(stack);
      if (ench.isEmpty()) {
         return 0;
      }
      if (!want.isEmpty() && !this.matchesWanted(ench, want)) {
         return 1;
      }
      nodes.add(new Node(false, 0, ench));
      return 0;
   }

   private List<String> parseWanted() {
      List<String> out = new ArrayList<>();
      for (String t : this.wanted.get().split("[,]+")) {
         String s = t.trim().toLowerCase(Locale.ROOT);
         if (!s.isEmpty()) {
            out.add(s);
         }
      }
      return out;
   }

   /** A book matches if any of its enchant ids or display names contains a keyword. */
   private boolean matchesWanted(Map<String, Integer> ench, List<String> want) {
      for (String id : ench.keySet()) {
         String name = this.display.getOrDefault(id, id).toLowerCase(Locale.ROOT);
         for (String kw : want) {
            if (id.contains(kw) || name.contains(kw)) {
               return true;
            }
         }
      }
      return false;
   }

   /** @return "A + B" of the first mutually-exclusive pair among all enchants, or null. */
   private String findConflict(List<Node> nodes) {
      LinkedHashSet<String> ids = new LinkedHashSet<>();
      for (Node n : nodes) {
         ids.addAll(n.ench.keySet());
      }
      List<String> list = new ArrayList<>(ids);
      for (int i = 0; i < list.size(); ++i) {
         for (int j = i + 1; j < list.size(); ++j) {
            class_6880<class_1887> a = this.entryById.get(list.get(i));
            class_6880<class_1887> b = this.entryById.get(list.get(j));
            if (a != null && b != null && !class_1890.method_8201(List.of(a), b)) {
               return this.display.getOrDefault(list.get(i), list.get(i)) + " §c+ §r"
                  + this.display.getOrDefault(list.get(j), list.get(j));
            }
         }
      }
      return null;
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
         class_6880<class_1887> entry = e.getKey();
         class_1887 ench = (class_1887)entry.comp_349();
         String id = entry.method_55840().toLowerCase(Locale.ROOT); // e.g. "minecraft:sharpness"
         m.put(id, e.getIntValue());
         this.anvilCost.put(id, ench.method_58446());
         this.maxLevel.put(id, ench.method_8183());
         this.display.put(id, ench.comp_2686().getString());
         this.entryById.put(id, entry);
      }
      return m;
   }

   /** "(Sharpness V, Mending)" using display names; empty maps render as "". */
   private String label(Map<String, Integer> ench) {
      if (ench.isEmpty()) {
         return "";
      }
      List<String> parts = new ArrayList<>(ench.size());
      for (Map.Entry<String, Integer> e : ench.entrySet()) {
         parts.add(this.enchName(e.getKey()) + (e.getValue() > 1 ? " " + RenderUtil.roman(e.getValue()) : ""));
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

   /** Whether the held stack is the item a preset targets (e.g. any *_boots for "boots"). */
   private static boolean itemMatches(class_1799 held, String key) {
      if (held.method_7960()) {
         return false;
      }
      String id = itemId(held);
      return id.equals(key) || id.endsWith("_" + key);
   }

   private static String itemId(class_1799 stack) {
      return class_7923.field_41178.method_10221(stack.method_7909()).method_12832();
   }

   // --- presets -----------------------------------------------------------

   private static Map<String, String> buildNames() {
      Map<String, String> m = new HashMap<>();
      m.put("minecraft:unbreaking", "Прочность");
      m.put("minecraft:mending", "Починка");
      m.put("minecraft:vanishing_curse", "Проклятие утраты");
      m.put("minecraft:protection", "Защита");
      m.put("minecraft:fire_protection", "Огнеупорность");
      m.put("minecraft:blast_protection", "Взрывоустойчивость");
      m.put("minecraft:projectile_protection", "Защита от снарядов");
      m.put("minecraft:thorns", "Шипы");
      m.put("minecraft:respiration", "Подводное дыхание");
      m.put("minecraft:aqua_affinity", "Подводник");
      m.put("minecraft:swift_sneak", "Проворство");
      m.put("minecraft:feather_falling", "Невесомость");
      m.put("minecraft:depth_strider", "Подводная ходьба");
      m.put("minecraft:frost_walker", "Ледоход");
      m.put("minecraft:soul_speed", "Скорость души");
      m.put("minecraft:binding_curse", "Проклятие несъёмности");
      m.put("minecraft:sharpness", "Острота");
      m.put("minecraft:smite", "Небесная кара");
      m.put("minecraft:bane_of_arthropods", "Бич членистоногих");
      m.put("minecraft:fire_aspect", "Заговор огня");
      m.put("minecraft:looting", "Добыча");
      m.put("minecraft:knockback", "Отдача");
      m.put("minecraft:sweeping_edge", "Разящий клинок");
      m.put("minecraft:density", "Плотность");
      m.put("minecraft:breach", "Пробитие");
      m.put("minecraft:wind_burst", "Порыв ветра");
      m.put("minecraft:power", "Сила");
      m.put("minecraft:punch", "Откидывание");
      m.put("minecraft:flame", "Воспламенение");
      m.put("minecraft:infinity", "Бесконечность");
      m.put("minecraft:multishot", "Тройной выстрел");
      m.put("minecraft:piercing", "Пронзающая стрела");
      m.put("minecraft:quick_charge", "Быстрая перезарядка");
      m.put("minecraft:loyalty", "Верность");
      m.put("minecraft:riptide", "Тягун");
      m.put("minecraft:channeling", "Громовержец");
      m.put("minecraft:impaling", "Пронзатель");
      m.put("minecraft:efficiency", "Эффективность");
      m.put("minecraft:silk_touch", "Шёлковое касание");
      m.put("minecraft:fortune", "Удача");
      m.put("minecraft:luck_of_the_sea", "Везучий рыбак");
      m.put("minecraft:lure", "Приманка");
      return m;
   }

   private static Map<String, Preset> buildPresets() {
      Map<String, Preset> m = new LinkedHashMap<>();
      m.put("helmet", preset("Топ шлем", "шлем", "protection", 4, "respiration", 3, "aqua_affinity", 1, "thorns", 3, "unbreaking", 3, "mending", 1));
      m.put("chestplate", preset("Топ нагрудник", "нагрудник", "protection", 4, "thorns", 3, "unbreaking", 3, "mending", 1));
      m.put("leggings", preset("Топ поножи", "поножи", "protection", 4, "swift_sneak", 3, "unbreaking", 3, "mending", 1));
      m.put("boots", preset("Топ ботинки", "ботинки", "protection", 4, "feather_falling", 4, "depth_strider", 3, "soul_speed", 3, "unbreaking", 3, "mending", 1));
      m.put("sword", preset("Топ меч", "меч", "sharpness", 5, "looting", 3, "sweeping_edge", 3, "fire_aspect", 2, "knockback", 2, "unbreaking", 3, "mending", 1));
      m.put("pickaxe", preset("Топ кирка", "кирку", "efficiency", 5, "fortune", 3, "unbreaking", 3, "mending", 1));
      m.put("axe", preset("Топ топор", "топор", "efficiency", 5, "sharpness", 5, "unbreaking", 3, "mending", 1));
      m.put("shovel", preset("Топ лопата", "лопату", "efficiency", 5, "fortune", 3, "unbreaking", 3, "mending", 1));
      m.put("bow", preset("Топ лук", "лук", "power", 5, "flame", 1, "punch", 2, "infinity", 1, "unbreaking", 3));
      m.put("crossbow", preset("Топ арбалет", "арбалет", "quick_charge", 3, "multishot", 1, "unbreaking", 3, "mending", 1));
      m.put("mace", preset("Топ булава", "булаву", "density", 5, "fire_aspect", 2, "wind_burst", 3, "unbreaking", 3, "mending", 1));
      m.put("trident", preset("Топ трезубец", "трезубец", "impaling", 5, "loyalty", 3, "channeling", 1, "unbreaking", 3, "mending", 1));
      m.put("fishing_rod", preset("Топ удочка", "удочку", "luck_of_the_sea", 3, "lure", 3, "unbreaking", 3, "mending", 1));
      m.put("elytra", preset("Топ элитры", "элитры", "unbreaking", 3, "mending", 1));
      m.put("shield", preset("Топ щит", "щит", "unbreaking", 3, "mending", 1));
      return m;
   }

   /** Builds a preset from labels and (short-id, level) pairs. */
   private static Preset preset(String ru, String item, Object... pairs) {
      LinkedHashMap<String, Integer> ench = new LinkedHashMap<>();
      for (int i = 0; i + 1 < pairs.length; i += 2) {
         ench.put("minecraft:" + pairs[i], (Integer)pairs[i + 1]);
      }
      return new Preset(ru, item, ench);
   }

   private static final class Preset {
      final String ru;
      final String item;
      final Map<String, Integer> ench;

      Preset(String ru, String item, Map<String, Integer> ench) {
         this.ru = ru;
         this.item = item;
         this.ench = ench;
      }
   }

   /** Planning node: an item's enchantments (by id) plus its anvil work count. */
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
