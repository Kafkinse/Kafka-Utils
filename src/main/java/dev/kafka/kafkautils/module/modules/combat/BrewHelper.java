package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.ChatUtil;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.class_1291;
import net.minecraft.class_1293;
import net.minecraft.class_1842;
import net.minecraft.class_2960;
import net.minecraft.class_6880;
import net.minecraft.class_7923;

/**
 * Brewing advisor. The player picks a potion and the exact brewing order is
 * printed to chat, including splash (взрывное) and lingering (оседающее) variants
 * plus extended (Redstone) / upgraded (Glowstone) modifiers. Trigger with
 * {@code /kafka brew <potion> [splash|lingering] [long|strong]}.
 *
 * <p>Pure recipe data — no world access. Corrupted potions (Invisibility,
 * Slowness, Harming, Weakness) already include their Fermented-Spider-Eye step;
 * modifiers that a potion cannot take (e.g. extending an instant potion) are
 * reported instead of shown.
 */
public class BrewHelper extends Module {
   public static final int DRINK = 0;
   public static final int SPLASH = 1;
   public static final int LINGERING = 2;

   private static final Map<String, Brew> BREWS = buildBrews();

   private static boolean openRequested;

   public BrewHelper() {
      super("Brew Helper", "Меню варки зелий: взрывные, оседающие, поиск (/kafka brew).", Category.FARMING);
   }

   protected void onEnable() {
      if (mc.field_1724 != null) {
         requestOpen(); // open the potion browser on the next client tick
      }
   }

   /** Ask the client to open the potion browser GUI (consumed in the tick loop). */
   public static void requestOpen() {
      openRequested = true;
   }

   public static boolean consumeOpen() {
      boolean r = openRequested;
      openRequested = false;
      return r;
   }

   public List<String> brewKeys() {
      return new ArrayList<>(BREWS.keySet());
   }

   public String brewTooltip(String key) {
      Brew b = BREWS.get(key);
      return b == null ? key : b.ru;
   }

   /** Prints the brewing order for {@code key}, honouring option keywords. */
   public void printBrew(String key, String options) {
      if (!BREWS.containsKey(key.toLowerCase(Locale.ROOT))) {
         ChatUtil.info("§d[Варка] §7неизвестно «" + key + "». Доступно: §r" + String.join(", ", BREWS.keySet()));
         return;
      }
      String o = options.toLowerCase(Locale.ROOT);
      int type = o.contains("linger") || o.contains("оседа") ? LINGERING
         : (o.contains("splash") || o.contains("взрыв") ? SPLASH : DRINK);
      boolean ext = o.contains("long") || o.contains("удлин") || o.contains("длит") || o.contains("ext");
      boolean upg = o.contains("strong") || o.contains("усил") || o.contains("amp") || o.contains(" ii") || o.contains("2");
      for (String line : this.recipeLines(key, type, ext, upg)) {
         ChatUtil.info(line);
      }
   }

   /** Prints the given recipe to chat (used by the GUI's "В чат" button). */
   public void printRecipe(String key, int type, boolean ext, boolean upg) {
      for (String line : this.recipeLines(key, type, ext, upg)) {
         ChatUtil.info(line);
      }
   }

   /** Builds the brewing-order lines for the given potion + variant (used by chat and the GUI). */
   public List<String> recipeLines(String key, int type, boolean ext, boolean upg) {
      List<String> out = new ArrayList<>();
      Brew b = BREWS.get(key.toLowerCase(Locale.ROOT));
      if (b == null) {
         out.add("§7неизвестно");
         return out;
      }

      List<String> warns = new ArrayList<>();
      if (ext && upg) {
         upg = false;
         warns.add("Нельзя продлить и усилить одновременно — беру продление.");
      }
      String lk = key.toLowerCase(Locale.ROOT);
      if (ext && potionEntry("long_" + lk) == null) {
         ext = false;
         warns.add("Это зелье нельзя продлить (Редстоун).");
      }
      if (upg && potionEntry("strong_" + lk) == null) {
         upg = false;
         warns.add("Это зелье нельзя усилить до II (Светокаменная пыль).");
      }

      String typeRu = type == SPLASH ? "Взрывное" : type == LINGERING ? "Оседающее" : "Питьевое";
      String modRu = ext ? " продлённое" : upg ? " усиленное II" : "";

      out.add("§d§l— Варка: " + b.ru + " §r§7(" + typeRu + modRu + ")");
      out.add("§7Топливо: §rОгненный порошок §7в левый слот.");
      int n = 1;
      out.add("§7" + n++ + ". §rНалей §eПузырёк воды ×3 §7в нижние слоты.");
      out.add(step(n++, "Адский нарост", "Мутное зелье"));
      for (String[] s : b.steps) {
         out.add(step(n++, s[0], s[1]));
      }
      if (ext) {
         out.add(step(n++, "Редстоун", "Продлённое"));
      }
      if (upg) {
         out.add(step(n++, "Светокаменная пыль", "Усиленное II"));
      }
      if (type == SPLASH || type == LINGERING) {
         out.add(step(n++, "Порох", "Взрывное"));
      }
      if (type == LINGERING) {
         out.add(step(n++, "Драконье дыхание", "Оседающее"));
      }
      out.add("§aГотово: §f" + typeRu + modRu + " зелье «" + b.ru + "».");
      String summary = resultSummary(key, type, ext, upg);
      if (!summary.isEmpty()) {
         out.add("§bИтог: §f" + summary);
      }
      for (String w : warns) {
         out.add("§e⚠ " + w);
      }
      return out;
   }

   private static String step(int n, String ingredient, String result) {
      return "§7" + n + ". §r+ §e" + ingredient + " §7→ §r" + result;
   }

   /** Localised result effect(s) with duration/level, like the creative tooltip. */
   private static String resultSummary(String key, int type, boolean ext, boolean upg) {
      String id = (ext ? "long_" : upg ? "strong_" : "") + key.toLowerCase(Locale.ROOT);
      class_6880<class_1842> entry = potionEntry(id);
      if (entry == null) {
         entry = potionEntry(key.toLowerCase(Locale.ROOT));
      }
      if (entry == null) {
         return "";
      }
      // Java Edition: splash keeps the full duration (the ¾ reduction is Bedrock
      // only); lingering shows ¼ of the drinkable duration.
      double mult = type == LINGERING ? 0.25 : 1.0;
      List<String> parts = new ArrayList<>();
      for (class_1293 eff : entry.comp_349().method_8049()) {
         String name = ((class_1291)eff.method_5579().comp_349()).method_5560().getString();
         int amp = eff.method_5578();
         String lvl = amp > 0 ? " " + RenderUtil.roman(amp + 1) : "";
         int dur = eff.method_5584();
         if (eff.method_48559() || dur <= 20) {
            parts.add(name + lvl);
         } else {
            parts.add(name + lvl + " (" + fmtTime((int)(dur * mult) / 20) + ")");
         }
      }
      return String.join(", ", parts);
   }

   private static String fmtTime(int seconds) {
      return seconds / 60 + ":" + String.format(Locale.ROOT, "%02d", seconds % 60);
   }

   @SuppressWarnings("unchecked")
   private static class_6880<class_1842> potionEntry(String id) {
      Object ref = class_7923.field_41179.method_10223(class_2960.method_60656(id)).orElse(null);
      return (class_6880<class_1842>)ref;
   }

   // --- recipe data -------------------------------------------------------

   private static Map<String, Brew> buildBrews() {
      Map<String, Brew> m = new LinkedHashMap<>();
      // key, ru, then post-Awkward (ingredient, result) pairs.
      // Продлить/Усилить availability is detected from the game registry
      // (long_/strong_ variants), not hardcoded, so it stays correct for every
      // potion including the Trial Chamber ones.
      m.put("fire_resistance", brew("Огнестойкость", "Сгусток магмы", "Огнестойкость"));
      m.put("night_vision", brew("Ночное зрение", "Золотая морковь", "Ночное зрение"));
      m.put("invisibility", brew("Невидимость", "Золотая морковь", "Ночное зрение", "Приготовленный паучий глаз", "Невидимость"));
      m.put("swiftness", brew("Скорость", "Сахар", "Скорость"));
      m.put("slowness", brew("Медлительность", "Сахар", "Скорость", "Приготовленный паучий глаз", "Медлительность"));
      m.put("strength", brew("Сила", "Огненный порошок", "Сила"));
      m.put("weakness", brew("Слабость", "Приготовленный паучий глаз", "Слабость"));
      m.put("healing", brew("Лечение", "Сверкающий ломтик арбуза", "Лечение"));
      m.put("harming", brew("Урон", "Сверкающий ломтик арбуза", "Лечение", "Приготовленный паучий глаз", "Урон"));
      m.put("poison", brew("Отравление", "Паучий глаз", "Отравление"));
      m.put("regeneration", brew("Регенерация", "Слеза гаста", "Регенерация"));
      m.put("leaping", brew("Прыгучесть", "Кроличья лапка", "Прыгучесть"));
      m.put("slow_falling", brew("Плавное падение", "Мембрана фантома", "Плавное падение"));
      m.put("water_breathing", brew("Подводное дыхание", "Иглобрюх", "Подводное дыхание"));
      m.put("turtle_master", brew("Черепашья мощь", "Черепаший панцирь", "Черепашья мощь"));
      m.put("wind_charged", brew("Ветровой заряд", "Стержень бриза", "Ветровой заряд"));
      m.put("weaving", brew("Ткачество", "Паутина", "Ткачество"));
      m.put("oozing", brew("Склизкость", "Блок слизи", "Склизкость"));
      m.put("infested", brew("Заражение", "Камень", "Заражение"));
      return m;
   }

   private static Brew brew(String ru, String... pairs) {
      List<String[]> steps = new ArrayList<>();
      for (int i = 0; i + 1 < pairs.length; i += 2) {
         steps.add(new String[]{pairs[i], pairs[i + 1]});
      }
      return new Brew(ru, steps);
   }

   private static final class Brew {
      final String ru;
      final List<String[]> steps;

      Brew(String ru, List<String[]> steps) {
         this.ru = ru;
         this.steps = steps;
      }
   }
}
