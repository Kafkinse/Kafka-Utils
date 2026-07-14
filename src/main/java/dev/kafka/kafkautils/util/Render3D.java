package dev.kafka.kafkautils.util;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2338;

/**
 * World-space drawing helper used by the ESP/render modules.
 *
 * <p><b>Status on 1.21.11:</b> Minecraft's 1.21.5+ render rewrite replaced the
 * old immediate-mode drawing path this helper relied on (the
 * {@code RenderLayer.getLines()} / {@code VertexConsumer} / {@code Camera.getPos()}
 * API no longer exists — camera position is now the private field
 * {@code Camera.field_18712}, and render layers are driven by the new
 * {@code RenderPipeline} system). Porting the in-world box/line drawing to that
 * new pipeline needs to be done and verified against a real 1.21.11 toolchain, so
 * for now these methods are intentional no-ops: the mod builds and every HUD,
 * combat and chat module works, and the ESP modules keep their HUD panels — only
 * the in-world outlines/bars are temporarily not drawn.
 *
 * <p>The public API ({@link #begin}, {@link #end}, {@code drawBox}, {@code drawFilled},
 * {@code circle}) is stable, so implementing the pipeline later touches only this
 * file — no call sites change.
 */
public final class Render3D {

   private Render3D() {
   }

   public static void begin(WorldRenderContext ctx) {
      // no-op until the 1.21.11 render pipeline path is implemented
   }

   public static void end() {
      // no-op
   }

   public static void drawBox(class_2338 pos, int argb) {
      // no-op
   }

   public static void drawBox(class_238 box, int argb) {
      // no-op
   }

   public static void drawFilled(class_238 box, int argb) {
      // no-op
   }

   public static void circle(class_243 center, float radius, int argb) {
      // no-op
   }
}
