package dev.kafka.kafkautils.gui;

import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.module.modules.chat.EconomyTracker;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_11908;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

/**
 * Balance dashboard: current balance, spent/received over day/week/month, and a
 * scrollable, filterable list of every recorded transaction. Opened with
 * {@code /kafka money}.
 */
public class EconomyScreen extends class_437 {
   private static final int BG = 0xE6101014;
   private static final int HEADER = 0xFF1B1030;
   private static final int ACCENT = 0xFFB388FF;
   private static final int PANEL = 0x33000000;
   private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm");

   private final EconomyTracker eco;
   private int scroll;
   private int filter; // 0 = all, 1 = spend, 2 = income

   public EconomyScreen() {
      super(class_2561.method_43470("Экономика"));
      this.eco = ModuleManager.get(EconomyTracker.class);
   }

   private class_4185 btn(String label, int x, int y, int w, int h, Runnable r) {
      return class_4185.method_46430(class_2561.method_43470(label), (b) -> r.run()).method_46434(x, y, w, h).method_46431();
   }

   protected void method_25426() {
      int w = this.field_22789;
      this.method_37063(this.btn(this.filter == 0 ? "§f§lВсе" : "§7Все", 12, 92, 60, 14, () -> {
         this.filter = 0;
         this.scroll = 0;
         this.method_41843();
      }));
      this.method_37063(this.btn(this.filter == 1 ? "§c§lТраты" : "§7Траты", 76, 92, 60, 14, () -> {
         this.filter = 1;
         this.scroll = 0;
         this.method_41843();
      }));
      this.method_37063(this.btn(this.filter == 2 ? "§a§lДоход" : "§7Доход", 140, 92, 60, 14, () -> {
         this.filter = 2;
         this.scroll = 0;
         this.method_41843();
      }));
      this.method_37063(this.btn("§dВ чат", w - 150, 92, 66, 14, () -> {
         if (this.eco != null) {
            this.eco.printSummary();
         }
      }));
      this.method_37063(this.btn("§7Закрыть", w - 78, 92, 66, 14, () -> this.method_25419()));
   }

   public boolean method_25404(class_11908 key) {
      if (key.comp_4795() == GLFW.GLFW_KEY_ESCAPE) {
         this.method_25419();
         return true;
      }
      return super.method_25404(key);
   }

   public boolean method_25401(double mouseX, double mouseY, double horiz, double vert) {
      if (vert != 0) {
         this.scroll -= (int) Math.round(vert) * 3;
         if (this.scroll < 0) {
            this.scroll = 0;
         }
         return true;
      }
      return super.method_25401(mouseX, mouseY, horiz, vert);
   }

   public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
      int w = this.field_22789;
      int h = this.field_22790;
      ctx.method_25294(0, 0, w, h, BG);
      ctx.method_25294(0, 0, w, 30, HEADER);
      ctx.method_25294(0, 29, w, 31, ACCENT);
      ctx.method_51433(this.field_22793, "§5§lЭкономика", 12, 11, 0xFFD9C2FF, true);

      super.method_25394(ctx, mouseX, mouseY, delta);

      if (this.eco == null) {
         ctx.method_51433(this.field_22793, "§7Модуль Economy Tracker выключен.", 12, 44, 0xFF9A8FB0, true);
         return;
      }

      String cur = this.eco.currency();
      String bal = this.eco.balanceKnown() ? EconomyTracker.fmt(this.eco.balance()) : "?";
      ctx.method_51433(this.field_22793, "§7Баланс: §f§l" + bal + " " + cur, 12, 40, 0xFFFFFFFF, true);

      // Stat rows.
      ctx.method_25294(8, 52, w - 8, 88, PANEL);
      row(ctx, "Сегодня", this.eco.spentToday(), this.eco.incomeToday(), cur, 56);
      row(ctx, "Неделя", this.eco.spentWeek(), this.eco.incomeWeek(), cur, 66);
      row(ctx, "Месяц", this.eco.spentMonth(), this.eco.incomeMonth(), cur, 76);

      // Transaction list.
      List<EconomyTracker.Txn> show = new ArrayList<>();
      List<EconomyTracker.Txn> all = this.eco.transactions();
      for (int i = all.size() - 1; i >= 0; --i) {
         EconomyTracker.Txn t = all.get(i);
         if (this.filter == 1 && t.income()) {
            continue;
         }
         if (this.filter == 2 && !t.income()) {
            continue;
         }
         show.add(t);
      }

      int top = 112;
      int bottom = h - 10;
      int rows = Math.max(1, (bottom - top) / 10);
      int maxScroll = Math.max(0, show.size() - rows);
      if (this.scroll > maxScroll) {
         this.scroll = maxScroll;
      }
      if (show.isEmpty()) {
         ctx.method_51433(this.field_22793, "§7Пока нет операций. Поторгуй/переведи — появятся.", 12, top, 0xFF9A8FB0, true);
         return;
      }
      // Columns: time | amount (right-aligned) | type | note.
      int cTime = 12;
      int cAmtRight = 150;
      int cType = 158;
      int cNote = 252;
      int y = top;
      for (int i = this.scroll; i < show.size() && y < bottom; ++i) {
         EconomyTracker.Txn t = show.get(i);
         String when = TIME.format(Instant.ofEpochMilli(t.ts).atZone(ZoneId.systemDefault()));
         String amt = (t.income() ? "§a+" : "§c-") + EconomyTracker.fmt(t.amount) + " " + cur;
         String note;
         if (t.kind == EconomyTracker.Kind.TRANSFER_OUT) {
            note = "§8→ §7" + t.who;
         } else if (t.kind == EconomyTracker.Kind.TRANSFER_IN) {
            note = "§8← §7" + t.who;
         } else {
            note = (t.note.isEmpty() ? "" : "§r" + t.note) + (t.who.isEmpty() ? "" : " §8(" + t.who + ")");
         }
         ctx.method_51433(this.field_22793, "§8" + when, cTime, y, 0xFFE7DAF6, true);
         ctx.method_51433(this.field_22793, amt, cAmtRight - this.field_22793.method_1727(amt), y, 0xFFFFFFFF, true);
         ctx.method_51433(this.field_22793, "§7" + EconomyTracker.label(t.kind), cType, y, 0xFFE7DAF6, true);
         ctx.method_51433(this.field_22793, note, cNote, y, 0xFFE7DAF6, true);
         y += 10;
      }
      if (this.scroll < maxScroll || this.scroll > 0) {
         ctx.method_51433(this.field_22793, "§8колесо — прокрутка (" + show.size() + " операций)", w - 190, 40, 0xFF6A6080, true);
      }
   }

   private void row(class_332 ctx, String label, long spent, long income, String cur, int y) {
      ctx.method_51433(this.field_22793, "§d" + label, 14, y, 0xFFD9C2FF, true);
      ctx.method_51433(this.field_22793, "§7потрачено §c-" + EconomyTracker.fmt(spent) + " " + cur, 90, y, 0xFFE7DAF6, true);
      ctx.method_51433(this.field_22793, "§7получено §a+" + EconomyTracker.fmt(income) + " " + cur, 300, y, 0xFFE7DAF6, true);
   }

   public boolean method_25421() {
      return false;
   }
}
