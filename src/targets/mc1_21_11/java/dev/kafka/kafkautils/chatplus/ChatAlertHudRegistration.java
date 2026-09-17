package dev.kafka.kafkautils.chatplus;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.class_1041;
import net.minecraft.class_310;
import net.minecraft.class_332;

/** Registers the "someone said your name" toast as a HUD overlay, 1.21.11. */
public final class ChatAlertHudRegistration {
   private ChatAlertHudRegistration() {
   }

   public static void register() {
      HudRenderCallback.EVENT.register((HudRenderCallback) (context, tickCounter) -> render(context));
   }

   private static void render(class_332 context) {
      class_310 mc = class_310.method_1551();
      class_1041 window = mc.method_22683();
      ChatAlertHud.render(new CompatGraphics(context, mc.field_1772), window.method_4486());
   }
}
