package dev.kafka.kafkautils;

import dev.kafka.kafkautils.config.ConfigManager;
import dev.kafka.kafkautils.gui.ClickGuiScreen;
import dev.kafka.kafkautils.hud.HudManager;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.module.WorldRenderModule;
import dev.kafka.kafkautils.module.modules.chat.AntiSpam;
import dev.kafka.kafkautils.module.modules.chat.ChatPing;
import dev.kafka.kafkautils.module.modules.chat.ClanChatHighlight;
import dev.kafka.kafkautils.module.modules.chat.CoordinateShare;
import dev.kafka.kafkautils.module.modules.combat.InventorySorter;
import dev.kafka.kafkautils.module.modules.combat.StorageAutoSort;
import dev.kafka.kafkautils.util.Render3D;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
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
   private static class_304 shareCoordsKey;
   private boolean shareKeyWasDown = false;
   private static class_304 sortKey;
   private boolean sortKeyWasDown = false;

   public void onInitializeClient() {
      ModuleManager.init();
      ConfigManager.load();
      openGuiKey = KeyBindingHelper.registerKeyBinding(new class_304("key.kafkautils.open_gui", class_307.field_1668, 88, class_11900.field_62556));
      hideHudKey = KeyBindingHelper.registerKeyBinding(new class_304("key.kafkautils.hide_hud", class_307.field_1668, 261, class_11900.field_62556));
      shareCoordsKey = KeyBindingHelper.registerKeyBinding(new class_304("key.kafkautils.share_coords", class_307.field_1668, -1, class_11900.field_62556));
      sortKey = KeyBindingHelper.registerKeyBinding(new class_304("key.kafkautils.sort", class_307.field_1668, -1, class_11900.field_62556));
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
         class_3675.class_306 sk = KeyBindingHelper.getBoundKeyOf(shareCoordsKey);
         boolean sDown = sk.method_1442() == class_307.field_1668 && sk.method_1444() != -1 && client.method_22683() != null && class_3675.method_15987(client.method_22683(), sk.method_1444());
         if (sDown && !this.shareKeyWasDown && client.field_1755 == null) {
            CoordinateShare cs = (CoordinateShare)ModuleManager.get(CoordinateShare.class);
            if (cs != null && cs.isEnabled()) {
               cs.share();
            }
         }

         this.shareKeyWasDown = sDown;
         class_3675.class_306 sortk = KeyBindingHelper.getBoundKeyOf(sortKey);
         boolean sortDown = sortk.method_1442() == class_307.field_1668 && sortk.method_1444() != -1 && client.method_22683() != null && class_3675.method_15987(client.method_22683(), sortk.method_1444());
         if (sortDown && !this.sortKeyWasDown) {
            boolean sorted = false;
            StorageAutoSort storage = (StorageAutoSort)ModuleManager.get(StorageAutoSort.class);
            if (storage != null && storage.isEnabled()) {
               sorted = storage.sortNow();
            }
            if (!sorted) {
               InventorySorter inv = (InventorySorter)ModuleManager.get(InventorySorter.class);
               if (inv != null && inv.isEnabled()) {
                  inv.sortNow();
               }
            }
         }

         this.sortKeyWasDown = sortDown;
         if (client.field_1724 != null && client.field_1687 != null) {
            ModuleManager.onTick();
         }

      });
      WorldRenderEvents.BEFORE_DEBUG_RENDER.register((WorldRenderEvents.DebugRender)(context) -> {
         class_310 client = class_310.method_1551();
         if (client.field_1687 != null && client.field_1724 != null) {
            Render3D.begin(context);

            try {
               for(Module m : ModuleManager.getModules()) {
                  if (m.isEnabled() && m instanceof WorldRenderModule wrm) {
                     try {
                        wrm.onWorldRender(context);
                     } catch (Throwable t) {
                        m.setEnabled(false);
                        System.err.println("[KafkaUtils] Disabled '" + m.getName() + "' after a render error: " + String.valueOf(t));
                     }
                  }
               }
            } finally {
               Render3D.end();
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
            net.minecraft.class_2561 result = message;
            ChatPing ping = (ChatPing)ModuleManager.get(ChatPing.class);
            if (ping != null) {
               result = ping.process(result);
            }
            ClanChatHighlight clan = (ClanChatHighlight)ModuleManager.get(ClanChatHighlight.class);
            if (clan != null) {
               result = clan.process(result);
            }
            return result;
         }
      });
      ClientPlayConnectionEvents.JOIN.register((ClientPlayConnectionEvents.Join)(handler, sender, client) -> HudManager.onWorldJoin());
      System.out.println("[KafkaUtils] Initialized — open the menu with X (rebind in Options → Controls).");
   }
}
