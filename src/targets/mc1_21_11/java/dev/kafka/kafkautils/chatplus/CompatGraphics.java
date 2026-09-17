package dev.kafka.kafkautils.chatplus;

import net.minecraft.class_327;
import net.minecraft.class_332;

/** 1.21.11-specific drawing surface behind {@link ChatPlusScreenLogic}'s font-agnostic calls. */
final class CompatGraphics {
   private final class_332 ctx;
   private final class_327 font;

   CompatGraphics(class_332 ctx, class_327 font) {
      this.ctx = ctx;
      this.font = font;
   }

   void fill(int left, int top, int right, int bottom, int color) {
      this.ctx.method_25294(left, top, right, bottom, color);
   }

   void text(String text, int x, int y, int color) {
      this.ctx.method_51433(this.font, text, x, y, color, true);
   }

   void outline(int x, int y, int width, int height, int color) {
      this.ctx.method_25294(x, y, x + width, y + 1, color);
      this.ctx.method_25294(x, y + height - 1, x + width, y + height, color);
      this.ctx.method_25294(x, y, x + 1, y + height, color);
      this.ctx.method_25294(x + width - 1, y, x + width, y + height, color);
   }
}
