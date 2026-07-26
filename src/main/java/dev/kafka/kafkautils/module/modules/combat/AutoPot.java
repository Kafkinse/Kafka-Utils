package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import dev.kafka.kafkautils.util.PotionActions;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.class_1799;

/**
 * Offensive potion helper: throw a chosen potion (e.g. splash Weakness) from
 * anywhere in the inventory at what you look at — even while holding a sword —
 * either by keybind (default R) or from the potion wheel (default V). Auto-heal
 * lives in the separate Pot Heal module, so enabling one does not affect the
 * other or the item wheel.
 *
 * <p>When Auto Pot is enabled, the wheel key throws potions; when it is disabled,
 * that key opens the Fast Swap wheel instead (see the client keybind logic).
 */
public class AutoPot extends Module {
   private final StringSetting throwMatch = this.add(new StringSetting("Throw Potion", "weakness,слабост"));
   // Harmful potions (thrown forward at enemies). Everything else counts as
   // beneficial and is thrown at your feet. Keywords cover EN and RU item names;
   // chosen to avoid false hits (e.g. "slowness" won't match "Slow Falling").
   private final StringSetting offensive = this.add(new StringSetting("Offensive Potions",
      "weakness,poison,harming,harm,slowness,infested,oozing,weaving,wind,decay,wither,blind,nausea,"
      + "слаб,отравл,вред,урон,медлительн,замедл,заражени,склизк,ткачеств,ветров,иссушен,тлен,слепот,тошнот"));

   public AutoPot() {
      super("Auto Pot", "Бросок зелья по клавише (R) и кольцо метательных зелий (V).", Category.COMBAT);
   }

   /** Throw the configured offensive potion in the look direction (keybind R). */
   public void throwConfigured() {
      if (mc.field_1724 == null || mc.field_1761 == null) {
         return;
      }
      int slot = PotionActions.findThrowable(this.throwMatch.get());
      if (slot >= 0) {
         PotionActions.useFromInventory(slot, false);
      } else {
         ChatUtil.info("§d[AutoPot] §7нет метательного зелья по фильтру «" + this.throwMatch.get() + "».");
      }
   }

   /** Distinct throwable potion types in the inventory (for the selection wheel). */
   public List<Opt> listThrowable() {
      List<Opt> out = new ArrayList<>();
      Set<String> seen = new LinkedHashSet<>();
      if (mc.field_1724 == null) {
         return out;
      }
      for (int i = 0; i < 36; ++i) {
         class_1799 s = mc.field_1724.method_31548().method_5438(i);
         if (PotionActions.isThrowable(s)) {
            String name = s.method_7964().getString();
            if (seen.add(name.toLowerCase(Locale.ROOT))) {
               out.add(new Opt(i, s, name));
            }
         }
      }
      return out;
   }

   /**
    * Uses a picked wheel option: harmful potions (weakness, poison, …) are thrown
    * forward at what you look at, while beneficial ones (speed and other buffs)
    * are thrown at your feet so they land on you.
    */
   public void use(Opt opt) {
      boolean harmful = PotionActions.matches(opt.stack, this.offensive.get());
      PotionActions.useFromInventory(opt.index, !harmful);
   }

   /** A throwable potion option for the wheel: inventory index, icon stack, display name. */
   public static final class Opt {
      public final int index;
      public final class_1799 stack;
      public final String name;

      public Opt(int index, class_1799 stack, String name) {
         this.index = index;
         this.stack = stack;
         this.name = name;
      }
   }
}
