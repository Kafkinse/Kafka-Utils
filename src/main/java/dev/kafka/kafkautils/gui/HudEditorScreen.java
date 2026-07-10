package dev.kafka.kafkautils.gui;

import dev.kafka.kafkautils.config.ConfigManager;
import dev.kafka.kafkautils.hud.HudManager;
import java.util.Map;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;

public class HudEditorScreen extends class_437 {
   private String dragging = null;
   private int dragOffX;
   private int dragOffY;

   public HudEditorScreen() {
      super(class_2561.method_43470("Kafka HUD Editor"));
   }

   public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
      ctx.method_25294(0, 0, this.field_22789, this.field_22790, -1877737448);
      HudManager.render(ctx);

      for(Map.Entry<String, int[]> e : HudManager.currentBounds().entrySet()) {
         int[] b = (int[])e.getValue();
         if (b[2] > 0 && b[3] > 0) {
            int color = ((String)e.getKey()).equals(this.dragging) ? -6606593 : -9826899;
            this.border(ctx, b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, color);
         }
      }

      ctx.method_51433(this.field_22793, "§d§lHUD Editor §r§7— перетаскивай окна мышью. ESC — сохранить и выйти.", 10, 10, -1517825, true);
      super.method_25394(ctx, mouseX, mouseY, delta);
   }

   public boolean method_25402(class_11909 click, boolean doubled) {
      if (click.method_74245() == 0) {
         double mouseX = click.comp_4798();
         double mouseY = click.comp_4799();

         for(Map.Entry<String, int[]> e : HudManager.currentBounds().entrySet()) {
            int[] b = (int[])e.getValue();
            if (b[2] > 0 && b[3] > 0 && mouseX >= (double)b[0] && mouseX <= (double)(b[0] + b[2]) && mouseY >= (double)b[1] && mouseY <= (double)(b[1] + b[3])) {
               this.dragging = (String)e.getKey();
               this.dragOffX = (int)mouseX - b[0];
               this.dragOffY = (int)mouseY - b[1];
               return true;
            }
         }
      }

      return super.method_25402(click, doubled);
   }

   public boolean method_25403(class_11909 click, double offsetX, double offsetY) {
      if (this.dragging != null) {
         int nx = Math.max(0, Math.min(this.field_22789 - 20, (int)click.comp_4798() - this.dragOffX));
         int ny = Math.max(0, Math.min(this.field_22790 - 10, (int)click.comp_4799() - this.dragOffY));
         HudManager.setPosition(this.dragging, nx, ny);
         return true;
      } else {
         return super.method_25403(click, offsetX, offsetY);
      }
   }

   public boolean method_25406(class_11909 click) {
      this.dragging = null;
      return super.method_25406(click);
   }

   private void border(class_332 ctx, int x, int y, int w, int h, int color) {
      ctx.method_25294(x, y, x + w, y + 1, color);
      ctx.method_25294(x, y + h - 1, x + w, y + h, color);
      ctx.method_25294(x, y, x + 1, y + h, color);
      ctx.method_25294(x + w - 1, y, x + w, y + h, color);
   }

   public void method_25419() {
      ConfigManager.save();
      super.method_25419();
   }

   public boolean method_25421() {
      return false;
   }
}
