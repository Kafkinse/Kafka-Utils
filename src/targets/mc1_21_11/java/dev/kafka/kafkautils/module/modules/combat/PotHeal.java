package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.PotionActions;

/**
 * Auto-heal: when your HP drops to the threshold, throws a splash/lingering
 * healing potion at your feet (packet-style, from anywhere in the inventory).
 * Independent of Auto Pot, so it never occupies the item-wheel key — you can
 * auto-heal and still use the Fast Swap wheel on V.
 *
 * <p>NOTE: packet automation not exercisable in the build environment; the
 * feet-aim throw may need tuning against a live server.
 */
public class PotHeal extends Module {
   private final NumberSetting health = this.add(new NumberSetting("Heal Below HP", 10, 1, 19, 1));
   private final StringSetting healMatch = this.add(new StringSetting("Heal Potion", "healing,исцел"));
   private final NumberSetting cooldown = this.add(new NumberSetting("Cooldown Ticks", 15, 0, 100, 1));
   private final BooleanSetting healAtFeet = this.add(new BooleanSetting("Heal At Feet", true));

   private int cd;

   public PotHeal() {
      super("Pot Heal", "Авто-хил метательным зельем исцеления при низком HP.", Category.COMBAT);
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
         int slot = PotionActions.findThrowable(this.healMatch.get());
         if (slot >= 0) {
            PotionActions.useFromInventory(slot, this.healAtFeet.get());
            this.cd = Math.max(5, this.cooldown.get());
         }
      }
   }
}
