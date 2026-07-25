package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.class_1268;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_7923;

/**
 * Combat potion helper. Automatically throws a splash/lingering healing potion
 * at your feet when HP drops below a threshold, and lets you throw a chosen
 * potion (e.g. splash Weakness) from anywhere in the inventory at what you look
 * at — even while holding a sword. The potion is used "packet-style": it is
 * swapped into the off-hand, thrown, and swapped back in the same tick, so the
 * main hand (your weapon) is never disturbed.
 *
 * <p>Only throwable potions (splash/lingering) are used — that is the only kind
 * that can be consumed instantly from the inventory. Matching is by the potion's
 * display name, so the keyword works in whatever language your client shows
 * (e.g. "healing"/"исцел", "weakness"/"слабост").
 *
 * <p>NOTE: packet automation that cannot be exercised in the build environment;
 * mappings are verified for 1.21.11 but the feet-aim throw may need tuning
 * against a live server.
 */
public class AutoPot extends Module {
   private static final class_1268 OFF_HAND = class_1268.values()[1];

   private final NumberSetting health = this.add(new NumberSetting("Heal Below HP", 10, 1, 19, 1));
   private final StringSetting healMatch = this.add(new StringSetting("Heal Potion", "healing,исцел"));
   private final StringSetting throwMatch = this.add(new StringSetting("Throw Potion", "weakness,слабост"));
   private final NumberSetting cooldown = this.add(new NumberSetting("Cooldown Ticks", 15, 0, 100, 1));
   private final BooleanSetting healAtFeet = this.add(new BooleanSetting("Heal At Feet", true));

   private int cd;

   public AutoPot() {
      super("Auto Pot", "Авто-хил зельем исцеления + бросок зелий из инвентаря (пакетно).", Category.COMBAT);
   }

   protected void onEnable() {
      this.cd = 0;
   }

   public void onTick() {
      if (mc.field_1724 == null || mc.field_1761 == null || mc.field_1687 == null) {
         return;
      }
      if (this.cd > 0) {
         --this.cd;
         return;
      }
      if (mc.field_1724.method_6032() <= (float) this.health.get()) {
         int slot = this.findThrowable(this.healMatch.get());
         if (slot >= 0) {
            this.usePotionStack(slot, this.healAtFeet.get());
            this.cd = Math.max(5, this.cooldown.get());
         }
      }
   }

   /** Throw the configured offensive potion (e.g. weakness) in the look direction. */
   public void throwConfigured() {
      if (mc.field_1724 == null || mc.field_1761 == null) {
         return;
      }
      int slot = this.findThrowable(this.throwMatch.get());
      if (slot >= 0) {
         this.usePotionStack(slot, false);
      } else {
         ChatUtil.info("§d[AutoPot] §7нет метательного зелья по фильтру «" + this.throwMatch.get() + "».");
      }
   }

   /** Packet-throws a potion from any inventory slot via a temporary off-hand swap. */
   public void usePotionStack(int invIndex, boolean lookDown) {
      if (mc.field_1724 == null || mc.field_1761 == null || invIndex < 0 || invIndex > 35) {
         return;
      }
      int screenSlot = invIndex < 9 ? invIndex + 36 : invIndex; // inventory index -> screen slot
      int sync = mc.field_1724.field_7512.field_7763;
      float savedPitch = mc.field_1724.method_36455();
      if (lookDown) {
         mc.field_1724.method_36457(90.0F);
      }
      mc.field_1761.method_2906(sync, screenSlot, 40, class_1713.field_7791, mc.field_1724); // -> off-hand
      mc.field_1761.method_2919(mc.field_1724, OFF_HAND);                                    // throw
      mc.field_1761.method_2906(sync, screenSlot, 40, class_1713.field_7791, mc.field_1724); // <- back
      if (lookDown) {
         mc.field_1724.method_36457(savedPitch);
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
         if (isThrowable(s)) {
            String name = s.method_7964().getString();
            if (seen.add(name.toLowerCase(Locale.ROOT))) {
               out.add(new Opt(i, s, name));
            }
         }
      }
      return out;
   }

   /** Throws a picked wheel option in the look direction. */
   public void use(Opt opt) {
      this.usePotionStack(opt.index, false);
   }

   private int findThrowable(String csv) {
      for (int i = 0; i < 36; ++i) {
         class_1799 s = mc.field_1724.method_31548().method_5438(i);
         if (isThrowable(s) && matches(s, csv)) {
            return i;
         }
      }
      return -1;
   }

   private static boolean isThrowable(class_1799 s) {
      if (s == null || s.method_7960()) {
         return false;
      }
      String id = class_7923.field_41178.method_10221(s.method_7909()).method_12832();
      return id.equals("splash_potion") || id.equals("lingering_potion");
   }

   private static boolean matches(class_1799 s, String csv) {
      String name = s.method_7964().getString().toLowerCase(Locale.ROOT);
      for (String k : csv.split(",")) {
         String kk = k.trim().toLowerCase(Locale.ROOT);
         if (!kk.isEmpty() && name.contains(kk)) {
            return true;
         }
      }
      return false;
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
