package dev.kafka.kafkautils.chatplus;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** 26.2-specific drawing surface behind {@link ChatPlusScreenLogic}'s font-agnostic calls. */
final class CompatGraphics {
   private final GuiGraphicsExtractor graphics;
   private final Font font;

   CompatGraphics(GuiGraphicsExtractor graphics, Font font) {
      this.graphics = graphics;
      this.font = font;
   }

   void fill(int left, int top, int right, int bottom, int color) {
      this.graphics.fill(left, top, right, bottom, color);
   }

   void text(String text, int x, int y, int color) {
      this.graphics.text(this.font, text, x, y, color);
   }

   void outline(int x, int y, int width, int height, int color) {
      this.graphics.outline(x, y, width, height, color);
   }
}
