package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.ChatUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
   private static final int DRINK = 0;
   private static final int SPLASH = 1;
   private static final int LINGERING = 2;

   private static final Map<String, Brew> BREWS = buildBrews();

   public BrewHelper() {
      super("Brew Helper", "Порядок варки зелий, включая взрывные и оседающие (/kafka brew).", Category.FARMING);
   }

   protected void onEnable() {
      ChatUtil.info("§d[Варка] §7используй §r/kafka brew <зелье> §7[splash|lingering] [long|strong]");
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
      Brew b = BREWS.get(key.toLowerCase(Locale.ROOT));
      if (b == null) {
         ChatUtil.info("§d[Варка] §7неизвестно «" + key + "». Доступно: §r" + String.join(", ", BREWS.keySet()));
         return;
      }

      String o = options.toLowerCase(Locale.ROOT);
      int type = o.contains("linger") || o.contains("оседа") ? LINGERING
         : (o.contains("splash") || o.contains("взрыв") ? SPLASH : DRINK);
      boolean ext = o.contains("long") || o.contains("удлин") || o.contains("длит") || o.contains("ext");
      boolean upg = o.contains("strong") || o.contains("усил") || o.contains("amp") || o.contains(" ii") || o.contains("2");

      List<String> warns = new ArrayList<>();
      if (ext && upg) {
         upg = false;
         warns.add("нельзя удлинить и усилить одновременно — беру удлинение.");
      }
      if (ext && !b.ext) {
         ext = false;
         warns.add("это зелье нельзя удлинить (Красная пыль).");
      }
      if (upg && !b.upg) {
         upg = false;
         warns.add("это зелье нельзя усилить до II (Светящаяся пыль).");
      }

      String typeRu = type == SPLASH ? "Взрывное" : type == LINGERING ? "Оседающее" : "Питьевое";
      String modRu = ext ? " удлинённое" : upg ? " усиленное (II)" : "";

      ChatUtil.info("§d§l— Варка: " + b.ru + " §r§7(" + typeRu.toLowerCase(Locale.ROOT) + modRu + ")");
      ChatUtil.info("§7Топливо: §rОгненный порошок §7в левый слот.");

      int n = 1;
      ChatUtil.info("§7" + n++ + ". §rНалей §eпузырёк воды ×3 §7в нижние слоты.");
      ChatUtil.info(step(n++, "Адский нарост", "Мутное зелье"));
      for (String[] s : b.steps) {
         ChatUtil.info(step(n++, s[0], s[1]));
      }
      if (ext) {
         ChatUtil.info(step(n++, "Редстоун", "удлинённое"));
      }
      if (upg) {
         ChatUtil.info(step(n++, "Светокаменная пыль", "усиленное (II)"));
      }
      if (type == SPLASH || type == LINGERING) {
         ChatUtil.info(step(n++, "Порох", "взрывное"));
      }
      if (type == LINGERING) {
         ChatUtil.info(step(n++, "Драконье дыхание", "оседающее"));
      }

      ChatUtil.info("§aГотово: " + typeRu.toLowerCase(Locale.ROOT) + " зелье «" + b.ru + "»" + modRu + ".");
      for (String w : warns) {
         ChatUtil.info("§e⚠ " + w);
      }
   }

   private static String step(int n, String ingredient, String result) {
      return "§7" + n + ". §r+ §e" + ingredient + " §7→ §r" + result;
   }

   // --- recipe data -------------------------------------------------------

   private static Map<String, Brew> buildBrews() {
      Map<String, Brew> m = new LinkedHashMap<>();
      // key, ru, canExtend, canUpgrade, then post-Awkward (ingredient, result) pairs
      m.put("fire_resistance", brew("Огнестойкость", true, false, "Сгусток магмы", "Огнестойкость"));
      m.put("night_vision", brew("Ночное зрение", true, false, "Золотая морковь", "Ночное зрение"));
      m.put("invisibility", brew("Невидимость", true, false, "Золотая морковь", "Ночное зрение", "Приготовленный паучий глаз", "Невидимость"));
      m.put("swiftness", brew("Скорость", true, true, "Сахар", "Скорость"));
      m.put("slowness", brew("Медлительность", true, true, "Сахар", "Скорость", "Приготовленный паучий глаз", "Медлительность"));
      m.put("strength", brew("Сила", true, true, "Огненный порошок", "Сила"));
      m.put("weakness", brew("Слабость", true, false, "Приготовленный паучий глаз", "Слабость"));
      m.put("healing", brew("Лечение", false, true, "Сверкающий ломтик арбуза", "Лечение"));
      m.put("harming", brew("Урон", false, true, "Сверкающий ломтик арбуза", "Лечение", "Приготовленный паучий глаз", "Урон"));
      m.put("poison", brew("Отравление", true, true, "Паучий глаз", "Отравление"));
      m.put("regeneration", brew("Регенерация", true, true, "Слеза гаста", "Регенерация"));
      m.put("leaping", brew("Прыгучесть", true, true, "Кроличья лапка", "Прыгучесть"));
      m.put("slow_falling", brew("Плавное падение", true, false, "Мембрана фантома", "Плавное падение"));
      m.put("water_breathing", brew("Подводное дыхание", true, false, "Иглобрюх", "Подводное дыхание"));
      m.put("turtle_master", brew("Черепашья мощь", true, true, "Черепаший панцирь", "Черепашья мощь"));
      m.put("wind_charged", brew("Ветровой заряд", false, false, "Стержень бриза", "Ветровой заряд"));
      m.put("weaving", brew("Ткачество", false, false, "Паутина", "Ткачество"));
      m.put("oozing", brew("Склизкость", false, false, "Блок слизи", "Склизкость"));
      m.put("infested", brew("Заражение", false, false, "Камень", "Заражение"));
      return m;
   }

   private static Brew brew(String ru, boolean ext, boolean upg, String... pairs) {
      List<String[]> steps = new ArrayList<>();
      for (int i = 0; i + 1 < pairs.length; i += 2) {
         steps.add(new String[]{pairs[i], pairs[i + 1]});
      }
      return new Brew(ru, ext, upg, steps);
   }

   private static final class Brew {
      final String ru;
      final boolean ext;
      final boolean upg;
      final List<String[]> steps;

      Brew(String ru, boolean ext, boolean upg, List<String[]> steps) {
         this.ru = ru;
         this.ext = ext;
         this.upg = upg;
         this.steps = steps;
      }
   }
}
