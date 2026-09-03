package dev.kafka.kafkautils.util;

import java.lang.reflect.Constructor;
import java.util.Optional;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_2960;
import net.minecraft.class_327;
import net.minecraft.class_332;

/**
 * Draws UI text with the bundled Rubik TrueType font (anti-aliased) instead of
 * the default pixel font, so the ClickGUI reads as smoothly as the mock-ups.
 *
 * <p>Minecraft 1.21.11 replaced {@code Style.withFont(Identifier)} with a font
 * key type ({@code net.minecraft.class_11719$class_11721}). Its constructor is
 * not stably mapped, so the style is built reflectively and cached: if anything
 * about that type differs from what we expect, {@link #style} stays {@code null}
 * and every helper falls back to the vanilla string renderer — the mod keeps
 * working, it just looks like it did before.
 */
public final class KFont {
   private static boolean initialised;
   private static class_2583 regular;
   private static class_2583 medium;

   private KFont() {
   }

   private static void ensure() {
      if (initialised) {
         return;
      }
      initialised = true;
      regular = build("kafkautils", "ui");
      medium = build("kafkautils", "ui_medium");
   }

   private static class_2583 build(String namespace, String path) {
      try {
         class_2960 id = class_2960.method_60655(namespace, path);
         Class<?> fontKey = Class.forName("net.minecraft.class_11719$class_11721");
         Constructor<?> ctor = null;
         for (Constructor<?> c : fontKey.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length >= 1 && p[0] == class_2960.class) {
               ctor = c;
               if (p.length == 1) {
                  break; // prefer the plain (Identifier) constructor
               }
            }
         }
         if (ctor == null) {
            return null;
         }
         ctor.setAccessible(true);
         Class<?>[] params = ctor.getParameterTypes();
         Object[] args = new Object[params.length];
         args[0] = id;
         for (int i = 1; i < params.length; ++i) {
            args[i] = defaultFor(params[i]);
         }
         Object key = ctor.newInstance(args);
         return class_2583.field_24360.method_27704((net.minecraft.class_11719) key);
      } catch (Throwable t) {
         return null;
      }
   }

   private static Object defaultFor(Class<?> type) {
      if (type == boolean.class || type == Boolean.class) {
         return Boolean.FALSE;
      }
      if (type == Optional.class) {
         return Optional.empty();
      }
      if (type == int.class) {
         return 0;
      }
      if (type == float.class) {
         return 0.0F;
      }
      return null;
   }

   /** The cached custom-font style (regular or medium), or {@code null} if unavailable. */
   public static class_2583 style(boolean med) {
      ensure();
      return med ? medium : regular;
   }

   public static boolean available() {
      ensure();
      return regular != null;
   }

   /** Strips legacy §-formatting codes; the custom font renders raw glyphs. */
   public static String strip(String s) {
      if (s == null) {
         return "";
      }
      if (s.indexOf('§') < 0) {
         return s;
      }
      StringBuilder b = new StringBuilder(s.length());
      for (int i = 0; i < s.length(); ++i) {
         char c = s.charAt(i);
         if (c == '§' && i + 1 < s.length()) {
            ++i; // skip the code char too
         } else {
            b.append(c);
         }
      }
      return b.toString();
   }

   private static class_2561 text(String s, boolean med) {
      class_2583 st = style(med);
      return class_2561.method_43470(strip(s)).method_10862(st);
   }

   /** Draws {@code s} at (x, y) in the custom font, or the vanilla font as a fallback. */
   public static void draw(class_332 ctx, class_327 tr, String s, int x, int y, int color, boolean shadow, boolean med) {
      if (style(med) != null) {
         ctx.method_51439(tr, text(s, med), x, y, color, shadow);
      } else {
         ctx.method_51433(tr, strip(s), x, y, color, shadow);
      }
   }

   /** Width of {@code s} as it will actually be drawn (custom font when available). */
   public static int width(class_327 tr, String s, boolean med) {
      if (style(med) != null) {
         return tr.method_27525(text(s, med));
      }
      return tr.method_1727(strip(s));
   }
}
