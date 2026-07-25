package dev.kafka.kafkautils.gui;

import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.module.modules.combat.AutoPot;
import java.util.List;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;

/**
 * Radial potion picker. Opened by a keybind; shows one icon per distinct
 * throwable potion in the inventory. Move the mouse toward a slice to highlight
 * it, click to throw that potion in the look direction (via {@link AutoPot}).
 */
public class PotionWheelScreen extends class_437 {
   private final AutoPot pot;
   private final List<AutoPot.Opt> opts;
   private int hovered = -1;

   public PotionWheelScreen() {
      super(class_2561.method_43470("Potion Wheel"));
      this.pot = ModuleManager.get(AutoPot.class);
      this.opts = this.pot != null ? this.pot.listThrowable() : List.of();
   }

   public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
      ctx.method_25294(0, 0, this.field_22789, this.field_22790, 0x99000000);
      int cx = this.field_22789 / 2;
      int cy = this.field_22790 / 2;
      ctx.method_51433(this.field_22793, "§d§lЗелья", cx - this.field_22793.method_1727("Зелья") / 2, cy - 5, 0xFFFFFFFF, true);

      if (this.opts.isEmpty()) {
         String m = "§7нет метательных зелий в инвентаре";
         ctx.method_51433(this.field_22793, m, cx - this.field_22793.method_1727(m) / 2, cy + 12, 0xFFFFFFFF, true);
         return;
      }

      int n = this.opts.size();
      double dx = mouseX - cx;
      double dy = mouseY - cy;
      double dist = Math.sqrt(dx * dx + dy * dy);
      this.hovered = -1;
      if (dist > 22) {
         double a = Math.atan2(dy, dx) + Math.PI / 2.0; // 0 = top
         if (a < 0) {
            a += Math.PI * 2.0;
         }
         this.hovered = (int) Math.round(a / (Math.PI * 2.0) * n) % n;
      }

      int radius = Math.min(130, 46 + n * 6);
      for (int i = 0; i < n; ++i) {
         double a = (Math.PI * 2.0 * i / n) - Math.PI / 2.0;
         int x = cx + (int) (Math.cos(a) * radius);
         int y = cy + (int) (Math.sin(a) * radius);
         boolean hi = i == this.hovered;
         ctx.method_25294(x - 13, y - 13, x + 13, y + 13, hi ? 0xFFB388FF : 0x88101014);
         ctx.method_51427(this.opts.get(i).stack, x - 8, y - 8);
         if (hi) {
            String nm = this.opts.get(i).name;
            ctx.method_51433(this.field_22793, nm, cx - this.field_22793.method_1727(nm) / 2, cy + 14, 0xFFFFFFFF, true);
         }
      }
   }

   public boolean method_25402(class_11909 click, boolean doubled) {
      if (click.method_74245() == 0 && this.hovered >= 0 && this.hovered < this.opts.size() && this.pot != null) {
         this.pot.use(this.opts.get(this.hovered));
         class_310.method_1551().method_1507((class_437) null);
         return true;
      }
      return super.method_25402(click, doubled);
   }

   public boolean method_25421() {
      return false;
   }
}
