package dev.kafka.kafkautils.gui;

import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.module.modules.combat.FastSwap;
import java.util.List;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;

/**
 * Radial item picker for {@link FastSwap}: one slice per distinct item type in
 * the inventory. Move the mouse toward a slice to highlight it, click to move
 * that item into your active hand slot (nothing is used or thrown).
 */
public class FastSwapScreen extends class_437 {
   private final FastSwap mod;
   private final List<FastSwap.Opt> opts;
   private int hovered = -1;

   public FastSwapScreen() {
      super(class_2561.method_43470("Fast Swap"));
      this.mod = ModuleManager.get(FastSwap.class);
      this.opts = this.mod != null ? this.mod.listItems() : List.of();
   }

   public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
      ctx.method_25294(0, 0, this.field_22789, this.field_22790, 0x99000000);
      int cx = this.field_22789 / 2;
      int cy = this.field_22790 / 2;
      ctx.method_51433(this.field_22793, "§d§lВ руку", cx - this.field_22793.method_1727("В руку") / 2, cy - 5, 0xFFFFFFFF, true);

      if (this.opts.isEmpty()) {
         String m = "§7инвентарь пуст";
         ctx.method_51433(this.field_22793, m, cx - this.field_22793.method_1727(m) / 2, cy + 12, 0xFFFFFFFF, true);
         return;
      }

      int n = this.opts.size();
      double dx = mouseX - cx;
      double dy = mouseY - cy;
      double dist = Math.sqrt(dx * dx + dy * dy);
      this.hovered = -1;
      if (dist > 22) {
         double a = Math.atan2(dy, dx) + Math.PI / 2.0;
         if (a < 0) {
            a += Math.PI * 2.0;
         }
         this.hovered = (int) Math.round(a / (Math.PI * 2.0) * n) % n;
      }

      int radius = Math.min(150, 46 + n * 5);
      for (int i = 0; i < n; ++i) {
         double a = (Math.PI * 2.0 * i / n) - Math.PI / 2.0;
         int x = cx + (int) (Math.cos(a) * radius);
         int y = cy + (int) (Math.sin(a) * radius);
         boolean hi = i == this.hovered;
         ctx.method_25294(x - 13, y - 13, x + 13, y + 13, hi ? 0xFFB388FF : 0x88101014);
         ctx.method_51427(this.opts.get(i).stack, x - 8, y - 8);
         int cnt = this.opts.get(i).count;
         if (cnt > 1) {
            ctx.method_51433(this.field_22793, "§f" + cnt, x + 2, y + 5, 0xFFFFFFFF, true);
         }
         if (hi) {
            String nm = this.opts.get(i).name;
            ctx.method_51433(this.field_22793, nm, cx - this.field_22793.method_1727(nm) / 2, cy + 14, 0xFFFFFFFF, true);
         }
      }
   }

   public boolean method_25402(class_11909 click, boolean doubled) {
      if (click.method_74245() == 0 && this.hovered >= 0 && this.hovered < this.opts.size() && this.mod != null) {
         this.mod.swapToHand(this.opts.get(this.hovered).index);
         class_310.method_1551().method_1507((class_437) null);
         return true;
      }
      return super.method_25402(click, doubled);
   }

   public boolean method_25421() {
      return false;
   }
}
