package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.mixin.ClientBossBarHudAccessor;
import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import dev.kafka.kafkautils.util.ItemUseHelper;
import java.util.Locale;
import net.minecraft.class_1294;
import net.minecraft.class_1802;
import net.minecraft.class_345;

/**
 * All-in-one raid helper (merges the old Auto Raid + Auto-Bad Omen). It finds an
 * Ominous Bottle in the hotbar on its own and drinks it to (re)apply Bad Omen —
 * after a raid ends (to start the next one) and/or whenever Bad Omen has run out.
 */
public class AutoRaid extends Module {
   private static final String[] KEYWORDS = {"рейд", "raid", "победа", "victory"};

   private final BooleanSetting renewAfterRaid = this.add(new BooleanSetting("Renew After Raid", true));
   private final BooleanSetting keepBadOmen = this.add(new BooleanSetting("Keep Bad Omen", true));

   private final ItemUseHelper use = ItemUseHelper.create();
   private boolean barPresentLastTick = false;
   private int cooldown = 0;

   public AutoRaid() {
      super("Auto Raid", "Auto-finds an Ominous Bottle and drinks it after a raid ends / to keep Bad Omen.", Category.COMBAT);
   }

   protected void onEnable() {
      this.barPresentLastTick = this.isRaidBarPresent();
      this.cooldown = 0;
   }

   public void onTick() {
      if (mc.field_1724 == null || mc.field_1705 == null) {
         return;
      }
      this.use.tick();
      if (this.cooldown > 0) {
         --this.cooldown;
      }

      boolean present = this.isRaidBarPresent();

      // Raid just ended -> renew for the next one.
      if (this.renewAfterRaid.get() && this.barPresentLastTick && !present) {
         this.drink("§dРейд завершён — пью Зловещий флакон");
      }
      this.barPresentLastTick = present;

      // Keep Bad Omen up while idle (no raid, no omen effect).
      if (this.keepBadOmen.get() && !present
         && !mc.field_1724.method_6059(class_1294.field_16595)
         && !mc.field_1724.method_6059(class_1294.field_50117)) {
         this.drink("§dПью Зловещий флакон (Bad Omen)");
      }
   }

   private void drink(String message) {
      if (this.cooldown > 0 || this.use.isBusy()) {
         return;
      }
      if (ItemUseHelper.hotbarHas(class_1802.field_50140) && this.use.startUsing(class_1802.field_50140, 35)) {
         ChatUtil.info(message);
         this.cooldown = 60;
      } else {
         this.cooldown = 40; // throttle: no bottle in hotbar, try again shortly (no chat spam)
      }
   }

   protected void onDisable() {
      if (mc.field_1690 != null) {
         mc.field_1690.field_1904.method_23481(false);
      }
   }

   private boolean isRaidBarPresent() {
      ClientBossBarHudAccessor accessor = (ClientBossBarHudAccessor)mc.field_1705.method_1740();
      for (class_345 bar : accessor.getBossBars().values()) {
         String name = bar.method_5414().getString().toLowerCase(Locale.ROOT);
         for (String keyword : KEYWORDS) {
            if (name.contains(keyword)) {
               return true;
            }
         }
      }
      return false;
   }
}
