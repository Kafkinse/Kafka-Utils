package dev.kafka.kafkautils.util;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.class_1921;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2338;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import org.joml.Matrix4f;

/**
 * Shared world-space drawing used by the ESP/render modules. It is fed the
 * per-frame {@link WorldRenderContext} by the client's BEFORE_DEBUG_RENDER hook
 * (see {@code KafkaUtilsClient}) via {@link #begin} / {@link #end}, so the module
 * draw calls themselves stay context-free.
 *
 * <p>NOTE: the low-level vertex/render-layer calls in this class target the
 * 1.21.11 mappings. They are the one part of this mod that could not be
 * compile-verified in the build environment used to produce this change, because
 * the Fabric/Mojang Maven repositories the toolchain needs are blocked by the
 * environment's egress policy. If a name here does not resolve against the real
 * 1.21.11 intermediary jar, only this file needs adjusting — the public API
 * ({@code drawBox}, {@code drawFilled}, {@code circle}) matches every call site.
 */
public final class Render3D {
   private static WorldRenderContext context;
   private static double camX;
   private static double camY;
   private static double camZ;

   private Render3D() {
   }

   public static void begin(WorldRenderContext ctx) {
      context = ctx;
      class_243 cam = class_310.method_1551().field_1769.method_19325();
      camX = cam.field_1352;
      camY = cam.field_1351;
      camZ = cam.field_1350;
   }

   public static void end() {
      class_4597 consumers = context != null ? context.consumers() : null;
      if (consumers instanceof class_4597.class_4598 immediate) {
         immediate.method_22993();
      }
      context = null;
   }

   private static class_4588 lineBuffer() {
      if (context == null) {
         return null;
      }
      return context.consumers().getBuffer(class_1921.method_23594());
   }

   private static class_4588 fillBuffer() {
      if (context == null) {
         return null;
      }
      return context.consumers().getBuffer(class_1921.method_23583());
   }

   private static Matrix4f matrix() {
      class_4587 stack = new class_4587();
      stack.method_46416((float)(-camX), (float)(-camY), (float)(-camZ));
      return stack.method_23760().method_23761();
   }

   /** Outlines the block occupying {@code pos}. */
   public static void drawBox(class_2338 pos, int argb) {
      drawBox(new class_238(pos), argb);
   }

   /** Outlines an axis-aligned box in world coordinates. */
   public static void drawBox(class_238 box, int argb) {
      class_4588 buf = lineBuffer();
      if (buf == null) {
         return;
      }
      Matrix4f m = matrix();
      float x1 = (float)box.field_1323;
      float y1 = (float)box.field_1322;
      float z1 = (float)box.field_1321;
      float x2 = (float)box.field_1320;
      float y2 = (float)box.field_1325;
      float z2 = (float)box.field_1324;

      // Bottom rectangle.
      line(buf, m, x1, y1, z1, x2, y1, z1, argb);
      line(buf, m, x2, y1, z1, x2, y1, z2, argb);
      line(buf, m, x2, y1, z2, x1, y1, z2, argb);
      line(buf, m, x1, y1, z2, x1, y1, z1, argb);
      // Top rectangle.
      line(buf, m, x1, y2, z1, x2, y2, z1, argb);
      line(buf, m, x2, y2, z1, x2, y2, z2, argb);
      line(buf, m, x2, y2, z2, x1, y2, z2, argb);
      line(buf, m, x1, y2, z2, x1, y2, z1, argb);
      // Vertical edges.
      line(buf, m, x1, y1, z1, x1, y2, z1, argb);
      line(buf, m, x2, y1, z1, x2, y2, z1, argb);
      line(buf, m, x2, y1, z2, x2, y2, z2, argb);
      line(buf, m, x1, y1, z2, x1, y2, z2, argb);
   }

   /** Draws a translucent filled box in world coordinates. */
   public static void drawFilled(class_238 box, int argb) {
      class_4588 buf = fillBuffer();
      if (buf == null) {
         return;
      }
      Matrix4f m = matrix();
      float x1 = (float)box.field_1323;
      float y1 = (float)box.field_1322;
      float z1 = (float)box.field_1321;
      float x2 = (float)box.field_1320;
      float y2 = (float)box.field_1325;
      float z2 = (float)box.field_1324;

      quad(buf, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, argb); // north
      quad(buf, m, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, argb); // south
      quad(buf, m, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, argb); // west
      quad(buf, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, argb); // east
      quad(buf, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, argb); // top
      quad(buf, m, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, argb); // bottom
   }

   /** Draws a horizontal circle (line loop) centred on {@code center}. */
   public static void circle(class_243 center, float radius, int argb) {
      class_4588 buf = lineBuffer();
      if (buf == null) {
         return;
      }
      Matrix4f m = matrix();
      int segments = 40;
      float cx = (float)center.field_1352;
      float cy = (float)center.field_1351;
      float cz = (float)center.field_1350;
      float prevX = cx + radius;
      float prevZ = cz;
      for (int i = 1; i <= segments; ++i) {
         double angle = 2.0 * Math.PI * i / segments;
         float nx = cx + radius * (float)Math.cos(angle);
         float nz = cz + radius * (float)Math.sin(angle);
         line(buf, m, prevX, cy, prevZ, nx, cy, nz, argb);
         prevX = nx;
         prevZ = nz;
      }
   }

   private static void line(class_4588 buf, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      float dz = z2 - z1;
      float len = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (len == 0.0F) {
         return;
      }
      dx /= len;
      dy /= len;
      dz /= len;
      buf.method_22918(m, x1, y1, z1).method_39415(argb).method_23763(null, dx, dy, dz);
      buf.method_22918(m, x2, y2, z2).method_39415(argb).method_23763(null, dx, dy, dz);
   }

   private static void quad(class_4588 buf, Matrix4f m,
                            float x1, float y1, float z1, float x2, float y2, float z2,
                            float x3, float y3, float z3, float x4, float y4, float z4, int argb) {
      buf.method_22918(m, x1, y1, z1).method_39415(argb);
      buf.method_22918(m, x2, y2, z2).method_39415(argb);
      buf.method_22918(m, x3, y3, z3).method_39415(argb);
      buf.method_22918(m, x4, y4, z4).method_39415(argb);
   }
}
