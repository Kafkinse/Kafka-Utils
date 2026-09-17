package dev.kafka.kafkautils.util;

import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_5250;
import net.minecraft.class_5251;

/**
 * Rewrites pure black ({@code &0}) text color to white, recursively across a
 * component and its siblings, so nicknames that use black stay readable
 * against the tab list's dark background instead of disappearing into it.
 */
public final class NicknameColorFix {
   private static final int BLACK = 0x000000;
   private static final int WHITE = 0xFFFFFF;

   private NicknameColorFix() {
   }

   public static class_5250 whitenBlack(class_2561 component) {
      class_5250 copy = component.method_27662();
      copy.method_10862(whitenStyle(component.method_10866()));
      for (class_2561 sibling : component.method_10855()) {
         copy.method_10852(whitenBlack(sibling));
      }
      return copy;
   }

   private static class_2583 whitenStyle(class_2583 style) {
      class_5251 color = style.method_10973();
      return color != null && color.method_27716() == BLACK ? style.method_36139(WHITE) : style;
   }
}
