package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1268;
import net.minecraft.class_1297;
import net.minecraft.class_1703;
import net.minecraft.class_1713;
import net.minecraft.class_1728;
import net.minecraft.class_243;
import net.minecraft.class_332;
import net.minecraft.class_3988;

/**
 * Auto-trades with villagers/wandering traders (inspired by sebseb7's
 * autotrade-fabric). When a merchant screen is open it repeatedly re-selects and
 * collects the chosen trade; otherwise it can reach out and open the nearest
 * merchant.
 *
 * <p>Two requested extras: <b>Rotations</b> can be turned off so the client head
 * never snaps toward the villager, and <b>Reach</b> sets how far the player will
 * interact from.
 *
 * <p>NOTE: packet-level automation that couldn't be exercised in the build
 * environment — names are verified against 1.21.11 mappings, but the trade timing
 * (select vs. collect) may need tuning on a live server.
 */
public class AutoTrade extends Module implements HudModule {
   private static final class_1268 MAIN_HAND = class_1268.values()[0];

   private final NumberSetting tradeIndex = this.add(new NumberSetting("Trade Slot", 0, 0, 9, 1));
   private final NumberSetting delay = this.add(new NumberSetting("Delay", 4, 1, 40, 1));
   private final NumberSetting reach = this.add(new NumberSetting("Reach", 4, 3, 6, 1));
   private final BooleanSetting rotations = this.add(new BooleanSetting("Rotations", false));
   private final BooleanSetting autoOpen = this.add(new BooleanSetting("Auto Open", true));

   private int tickCounter;
   private int trades;
   private String status = "—";

   public AutoTrade() {
      super("Auto Trade", "Farms villager trades; rotations optional, adjustable reach.", Category.FARMING);
   }

   protected void onEnable() {
      this.trades = 0;
      this.tickCounter = 0;
      this.status = "—";
   }

   public void onTick() {
      if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) {
         return;
      }
      if (++this.tickCounter < this.delay.get()) {
         return;
      }
      this.tickCounter = 0;

      // A merchant screen is open — farm the selected trade.
      class_1703 handler = mc.field_1724.field_7512;
      if (handler instanceof class_1728 merchant) {
         merchant.method_20215(this.tradeIndex.get());
         mc.field_1761.method_2906(merchant.field_7763, 2, 0, class_1713.field_7794, mc.field_1724);
         ++this.trades;
         this.status = "торгую (слот " + this.tradeIndex.get() + ")";
         return;
      }

      // Otherwise reach out to the nearest merchant and open it.
      if (this.autoOpen.get()) {
         class_3988 target = this.nearestMerchant();
         if (target != null) {
            if (this.rotations.get()) {
               this.aimAt(target);
            }
            mc.field_1761.method_2905(mc.field_1724, target, MAIN_HAND);
            this.status = "открываю торговца";
         } else {
            this.status = "нет торговца рядом";
         }
      }
   }

   private class_3988 nearestMerchant() {
      class_3988 best = null;
      double bestSq = (double)(this.reach.get() * this.reach.get());
      for (class_1297 e : mc.field_1687.method_18112()) {
         if (e instanceof class_3988 merchant) {
            double sq = mc.field_1724.method_5858(e);
            if (sq <= bestSq) {
               bestSq = sq;
               best = merchant;
            }
         }
      }
      return best;
   }

   private void aimAt(class_1297 target) {
      class_243 eye = mc.field_1724.method_33571();
      double dx = target.method_23317() - eye.field_1352;
      double dy = (target.method_23318() + 1.0) - eye.field_1351;
      double dz = target.method_23321() - eye.field_1350;
      double distXZ = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
      float pitch = (float)(-Math.toDegrees(Math.atan2(dy, distXZ)));
      mc.field_1724.method_36456(yaw);
      mc.field_1724.method_36457(pitch);
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList<>();
      lines.add("§7Статус: §r" + this.status);
      lines.add("§7Сделок: §a" + this.trades);
      lines.add("§7Ротации: " + (this.rotations.get() ? "§aВКЛ" : "§7ВЫКЛ") + " §7| Reach: §r" + this.reach.get());
      return RenderUtil.panel(ctx, x, y, "Auto Trade", lines);
   }
}
