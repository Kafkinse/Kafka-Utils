package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.ModeSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_1268;
import net.minecraft.class_1297;
import net.minecraft.class_1703;
import net.minecraft.class_1713;
import net.minecraft.class_1728;
import net.minecraft.class_1799;
import net.minecraft.class_1914;
import net.minecraft.class_1916;
import net.minecraft.class_243;
import net.minecraft.class_332;
import net.minecraft.class_3988;
import net.minecraft.class_7923;

/**
 * Auto-trades with villagers/wandering traders (inspired by sebseb7's
 * autotrade-fabric).
 *
 * <p>With <b>Auto Open</b> on it runs the whole cycle by itself: walk to a
 * merchant, open it, execute the trades matching your Action + Items filter (up
 * to Trades/Visit), then close the menu and, after Reopen Delay, do it again.
 * Villagers that have nothing left to trade are remembered and skipped for
 * "Skip Sold-Out" seconds so it stops poking an exhausted one. With Auto Open off
 * it just farms a menu you opened yourself and never closes it.
 *
 * <p>Each trade is done in two steps a tick apart — select the recipe, then take
 * the result — so the output slot is filled before it's collected.
 *
 * <p>NOTE: packet-level automation, not exercised in the build environment.
 */
public class AutoTrade extends Module implements HudModule {
   private static final class_1268 MAIN_HAND = class_1268.values()[0];
   private static final int RESULT_SLOT = 2;
   private static final int SELECTED = 0;
   private static final int TOOK = 1;
   private static final int NONE = 2;

   private final ModeSetting action = this.add(new ModeSetting("Action", 0, "Buy", "Sell"));
   private final StringSetting items = this.add(new StringSetting("Items", "emerald"));
   private final BooleanSetting autoOpen = this.add(new BooleanSetting("Auto Open", true));
   private final NumberSetting tradesPerVisit = this.add(new NumberSetting("Trades/Visit", 12, 1, 64, 1));
   private final NumberSetting delay = this.add(new NumberSetting("Delay", 3, 1, 40, 1));
   private final NumberSetting reopenDelay = this.add(new NumberSetting("Reopen Delay", 20, 5, 100, 5));
   private final NumberSetting skipSoldOut = this.add(new NumberSetting("Skip Sold-Out", 60, 10, 600, 10));
   private final NumberSetting reach = this.add(new NumberSetting("Reach", 4, 3, 6, 1));
   private final BooleanSetting rotations = this.add(new BooleanSetting("Rotations", false));

   private final Map<UUID, Long> exhaustedUntil = new HashMap<>();
   private int tickCounter;
   private int visitCooldown;
   private int tradesThisVisit;
   private int selectedIndex = -1;
   private UUID currentVillager;
   private int trades;
   private String status = "—";

   public AutoTrade() {
      super("Auto Trade", "Fully auto villager trading: open, buy/sell, close, repeat.", Category.FARMING);
   }

   protected void onEnable() {
      this.trades = 0;
      this.tickCounter = 0;
      this.visitCooldown = 0;
      this.tradesThisVisit = 0;
      this.selectedIndex = -1;
      this.currentVillager = null;
      this.exhaustedUntil.clear();
      this.status = "—";
   }

   public void onTick() {
      if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) {
         return;
      }

      class_1703 handler = mc.field_1724.field_7512;
      boolean merchantOpen = handler instanceof class_1728;

      if (merchantOpen) {
         if (++this.tickCounter < this.delay.get()) {
            return;
         }
         this.tickCounter = 0;
         class_1728 merchant = (class_1728)handler;

         if (!this.autoOpen.get()) {
            this.tradeStep(merchant); // manual: farm, never close
            return;
         }
         if (this.tradesThisVisit >= this.tradesPerVisit.get()) {
            this.closeMenu();
            return;
         }
         int result = this.tradeStep(merchant);
         if (result == TOOK) {
            ++this.tradesThisVisit;
         } else if (result == NONE) {
            if (this.currentVillager != null) {
               this.exhaustedUntil.put(this.currentVillager, System.currentTimeMillis() + this.skipSoldOut.get() * 1000L);
            }
            this.closeMenu();
         }
         return;
      }

      // No merchant open.
      this.tradesThisVisit = 0;
      this.selectedIndex = -1;
      if (this.visitCooldown > 0) {
         --this.visitCooldown;
         this.status = "пауза";
         return;
      }
      if (++this.tickCounter < this.delay.get()) {
         return;
      }
      this.tickCounter = 0;

      if (this.autoOpen.get() && mc.field_1755 == null) {
         class_3988 target = this.nearestMerchant();
         if (target != null) {
            this.currentVillager = target.method_5667();
            if (this.rotations.get()) {
               this.aimAt(target);
            }
            mc.field_1761.method_2905(mc.field_1724, target, MAIN_HAND);
            this.status = "открываю торговца";
         } else {
            this.status = "нет торговца рядом";
         }
      } else {
         this.status = "ожидание меню";
      }
   }

   /** Two-phase: select a matching recipe, then (next call) take its result. */
   private int tradeStep(class_1728 merchant) {
      if (this.selectedIndex >= 0) {
         mc.field_1761.method_2906(merchant.field_7763, RESULT_SLOT, 0, class_1713.field_7794, mc.field_1724);
         ++this.trades;
         this.selectedIndex = -1;
         this.status = "забрал";
         return TOOK;
      }

      class_1916 recipes = merchant.method_17438();
      boolean buy = this.action.get().equals("Buy");
      for (int i = 0; i < recipes.size(); ++i) {
         class_1914 offer = recipes.get(i);
         if (offer.method_8255()) {
            continue; // out of stock / disabled
         }
         class_1799 key = buy ? offer.method_8250() : offer.method_19272();
         if (this.matchesItem(key)) {
            merchant.method_20215(i);
            this.selectedIndex = i;
            this.status = (buy ? "выбрал " : "продаю ") + itemId(key);
            return SELECTED;
         }
      }
      this.status = "нет подходящей сделки";
      return NONE;
   }

   private void closeMenu() {
      mc.field_1724.method_7346();
      this.tradesThisVisit = 0;
      this.selectedIndex = -1;
      this.visitCooldown = this.reopenDelay.get();
      this.status = "закрыл меню";
   }

   private boolean matchesItem(class_1799 stack) {
      String filter = this.items.get().trim();
      if (filter.isEmpty()) {
         return true; // no filter -> trade whatever is available
      }
      String id = itemId(stack);
      for (String t : filter.split("[,\\s]+")) {
         if (!t.isBlank() && id.contains(t.trim().toLowerCase(Locale.ROOT))) {
            return true;
         }
      }
      return false;
   }

   private class_3988 nearestMerchant() {
      long now = System.currentTimeMillis();
      class_3988 best = null;
      double bestSq = (double)(this.reach.get() * this.reach.get());
      for (class_1297 e : mc.field_1687.method_18112()) {
         if (e instanceof class_3988 merchant) {
            Long until = this.exhaustedUntil.get(e.method_5667());
            if (until != null && now < until) {
               continue; // skip a merchant we already emptied
            }
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

   private static String itemId(class_1799 stack) {
      return class_7923.field_41178.method_10221(stack.method_7909()).method_12832();
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList<>();
      lines.add("§7" + this.action.get() + ": §d" + this.items.get());
      lines.add("§7Статус: §r" + this.status);
      lines.add("§7Сделок: §a" + this.trades + " §7| пропущено: §r" + this.exhaustedUntil.size());
      lines.add("§7Авто: " + (this.autoOpen.get() ? "§aВКЛ" : "§7ВЫКЛ")
         + " §7Ротации: " + (this.rotations.get() ? "§aВКЛ" : "§7ВЫКЛ"));
      return RenderUtil.panel(ctx, x, y, "Auto Trade", lines);
   }
}
