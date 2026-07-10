package dev.kafka.kafkautils;

import dev.kafka.kafkautils.config.ConfigManager;
import dev.kafka.kafkautils.gui.ClickGuiScreen;
import dev.kafka.kafkautils.hud.HudManager;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.module.WorldRenderModule;
import dev.kafka.kafkautils.module.modules.chat.AntiSpam;
import dev.kafka.kafkautils.module.modules.chat.ChatPing;
import java.io.PrintStream;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.class_12180;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraft.class_437;
import net.minecraft.class_304.class_11900;
import net.minecraft.class_3675.class_307;

public class KafkaUtilsClient implements ClientModInitializer {
   public static final String MOD_ID = "kafkautils";
   private static class_304 openGuiKey;
   private boolean openKeyWasDown = false;
   private static class_304 hideHudKey;
   private boolean hideKeyWasDown = false;

   public void onInitializeClient() {
      ModuleManager.init();
      ConfigManager.load();
      openGuiKey = KeyBindingHelper.registerKeyBinding(new class_304("key.kafkautils.open_gui", class_307.field_1668, 88, class_11900.field_62556));
      hideHudKey = KeyBindingHelper.registerKeyBinding(new class_304("key.kafkautils.hide_hud", class_307.field_1668, 261, class_11900.field_62556));
      ClientTickEvents.END_CLIENT_TICK.register((ClientTickEvents.EndTick)(client) -> {
         class_3675.class_306 bound = KeyBindingHelper.getBoundKeyOf(openGuiKey);
         boolean down = bound.method_1442() == class_307.field_1668 && bound.method_1444() != -1 && client.method_22683() != null && class_3675.method_15987(client.method_22683(), bound.method_1444());
         if (down && !this.openKeyWasDown) {
            if (client.field_1755 == null) {
               client.method_1507(new ClickGuiScreen());
            } else if (client.field_1755 instanceof ClickGuiScreen) {
               client.method_1507((class_437)null);
            }
         }

         this.openKeyWasDown = down;
         class_3675.class_306 hk = KeyBindingHelper.getBoundKeyOf(hideHudKey);
         boolean hDown = hk.method_1442() == class_307.field_1668 && hk.method_1444() != -1 && client.method_22683() != null && class_3675.method_15987(client.method_22683(), hk.method_1444());
         if (hDown && !this.hideKeyWasDown && client.field_1755 == null) {
            HudManager.toggleHidden();
         }

         this.hideKeyWasDown = hDown;
         if (client.field_1724 != null && client.field_1687 != null) {
            ModuleManager.onTick();
         }

      });
      WorldRenderEvents.BEFORE_DEBUG_RENDER.register((WorldRenderEvents.DebugRender)(context) -> {
         class_310 client = class_310.method_1551();
         if (client.field_1769 != null) {
            try {
               class_12180.class_12181 scope = client.field_1769.method_75414();

               try {
                  for(Module m : ModuleManager.getModules()) {
                     if (m.isEnabled() && m instanceof WorldRenderModule) {
                        WorldRenderModule wrm = (WorldRenderModule)m;

                        try {
                           wrm.onWorldRender(context);
                        } catch (Throwable t) {
                           m.setEnabled(false);
                           PrintStream var10000 = System.err;
                           String var10001 = m.getName();
                           var10000.println("[KafkaUtils] Disabled '" + var10001 + "' after a render error: " + String.valueOf(t));
                        }
                     }
                  }
               } catch (Throwable var9) {
                  if (scope != null) {
                     try {
                        scope.close();
                     } catch (Throwable var7) {
                        var9.addSuppressed(var7);
                     }
                  }

                  throw var9;
               }

               if (scope != null) {
                  scope.close();
               }
            } catch (Throwable t) {
               System.err.println("[KafkaUtils] gizmo scope error: " + String.valueOf(t));
            }

         }
      });
      HudRenderCallback.EVENT.register((HudRenderCallback)(context, tickCounter) -> HudManager.render(context));
      ClientReceiveMessageEvents.ALLOW_GAME.register((ClientReceiveMessageEvents.AllowGame)(message, overlay) -> {
         AntiSpam as = (AntiSpam)ModuleManager.get(AntiSpam.class);
         return as == null || as.allow(message);
      });
      ClientReceiveMessageEvents.MODIFY_GAME.register((ClientReceiveMessageEvents.ModifyGame)(message, overlay) -> {
         if (overlay) {
            return message;
         } else {
            ChatPing ping = (ChatPing)ModuleManager.get(ChatPing.class);
            return ping != null ? ping.process(message) : message;
         }
      });
      ClientPlayConnectionEvents.JOIN.register((ClientPlayConnectionEvents.Join)(handler, sender, client) -> HudManager.onWorldJoin());
      System.out.println("[KafkaUtils] Initialized — open the menu with X (rebind in Options → Controls).");
   }
}
