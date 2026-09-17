package dev.kafka.kafkautils.util;

import java.util.List;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;

/**
 * Small 2D helpers shared by every HUD module: a titled text panel plus a
 * couple of formatting utilities (roman numerals and mm:ss timers).
 */
public final class RenderUtil {
   private static final int PADDING = 3;
   private static final int LINE_HEIGHT = 10;
   private static final int BG_COLOR = 0xB0000000;
   private static final int TITLE_BG = 0xC02A0A3A;
   private static final int TITLE_COLOR = 0xFFE8B0FF;
   private static final int TEXT_COLOR = 0xFFFFFFFF;

   private RenderUtil() {
   }

   /**
    * Draws a titled panel and returns its {width, height} so the HUD manager can
    * lay the next panel out below it.
    */
   public static int[] panel(class_332 ctx, int x, int y, String title, List<String> lines) {
      class_327 tr = class_310.method_1551().field_1772;

      int width = tr.method_1727(title);
      for (String line : lines) {
         width = Math.max(width, tr.method_1727(line));
      }
      width += PADDING * 2;

      int titleH = LINE_HEIGHT + 1;
      int height = titleH + PADDING + lines.size() * LINE_HEIGHT + PADDING;

      // Backdrop + title strip.
      ctx.method_25294(x, y, x + width, y + height, BG_COLOR);
      ctx.method_25294(x, y, x + width, y + titleH, TITLE_BG);

      // Title is centred within the panel; content lines are left-aligned.
      String titleText = "§l" + title;
      ctx.method_51433(tr, titleText, x + (width - tr.method_1727(titleText)) / 2, y + 2, TITLE_COLOR, true);

      int lineY = y + titleH + PADDING;
      for (String line : lines) {
         ctx.method_51433(tr, line, x + PADDING, lineY, TEXT_COLOR, true);
         lineY += LINE_HEIGHT;
      }

      return new int[]{width, height};
   }

   /** Converts 1..3999 to a roman numeral; anything outside is returned as-is. */
   public static String roman(int value) {
      if (value <= 0 || value >= 4000) {
         return Integer.toString(value);
      }

      int[] nums = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
      String[] syms = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < nums.length; ++i) {
         while (value >= nums[i]) {
            value -= nums[i];
            sb.append(syms[i]);
         }
      }
      return sb.toString();
   }

   /** Formats a duration in whole seconds as m:ss (or h:mm:ss when needed). */
   public static String time(int seconds) {
      if (seconds < 0) {
         seconds = 0;
      }
      int hours = seconds / 3600;
      int minutes = seconds % 3600 / 60;
      int secs = seconds % 60;
      if (hours > 0) {
         return String.format("%d:%02d:%02d", hours, minutes, secs);
      }
      return String.format("%d:%02d", minutes, secs);
   }
}
