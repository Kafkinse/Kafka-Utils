package dev.kafka.kafkautils.chatplus;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Registers the "someone said your name" toast as a HUD overlay, 26.2. */
public final class ChatAlertHudRegistration {
   private ChatAlertHudRegistration() {
   }

   public static void register() {
      HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("kafkautils", "chat_alerts"),
            (graphics, deltaTracker) -> render(graphics));
   }

   private static void render(GuiGraphicsExtractor graphics) {
      ChatAlertHud.render(new CompatGraphics(graphics, Minecraft.getInstance().font), graphics.guiWidth());
   }
}
