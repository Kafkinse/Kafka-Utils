package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.mixin.ClientBossBarHudAccessor;
import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.util.ChatUtil;
import dev.kafka.kafkautils.util.ItemUseHelper;
import java.util.Locale;
import net.minecraft.class_1802;
import net.minecraft.class_345;

public class AutoRaid extends Module {
   private static final String[] KEYWORDS = new String[]{"рейд", "raid", "победа", "victory"};
   private final ItemUseHelper use = ItemUseHelper.create();
   private boolean barPresentLastTick = false;

   public AutoRaid() {
      super("Auto Raid", "Drinks a raid potion when the raid bar appears/disappears.", Category.COMBAT);
   }

   protected void onEnable() {
      this.barPresentLastTick = this.isRaidBarPresent();
   }

   public void onTick() {
      if (mc.field_1724 != null && mc.field_1705 != null) {
         this.use.tick();
         boolean present = this.isRaidBarPresent();
         if (present != this.barPresentLastTick && !this.use.isBusy()) {
            if (this.use.startUsing(class_1802.field_8574, 35)) {
               ChatUtil.info(present ? "Рейд начался — пью зелье" : "Рейд завершён — пью зелье");
            } else {
               ChatUtil.info("Нет зелья в хотбаре для авто-рейда");
            }
         }

         this.barPresentLastTick = present;
      }
   }

   protected void onDisable() {
      if (mc.field_1690 != null) {
         mc.field_1690.field_1904.method_23481(false);
      }

   }

   private boolean isRaidBarPresent() {
      ClientBossBarHudAccessor accessor = (ClientBossBarHudAccessor)mc.field_1705.method_1740();

      for(class_345 bar : accessor.getBossBars().values()) {
         String name = bar.method_5414().getString().toLowerCase(Locale.ROOT);

         for(String keyword : KEYWORDS) {
            if (name.contains(keyword)) {
               return true;
            }
         }
      }

      return false;
   }
}
