package dev.kafka.kafkautils;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.kafka.kafkautils.config.ConfigManager;
import dev.kafka.kafkautils.gui.ClickGuiScreen;
import dev.kafka.kafkautils.hud.HudManager;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.module.WorldRenderModule;
import dev.kafka.kafkautils.module.modules.chat.AntiSpam;
import dev.kafka.kafkautils.module.modules.chat.ChatPing;
import dev.kafka.kafkautils.module.modules.chat.CoordinateShare;
import dev.kafka.kafkautils.module.modules.chat.FriendChat;
import dev.kafka.kafkautils.module.modules.chat.FriendHighlight;
import dev.kafka.kafkautils.module.modules.chat.FriendList;
import dev.kafka.kafkautils.module.modules.combat.EnchantHelper;
import dev.kafka.kafkautils.util.Render3D;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.class_2561;
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

   public void onInitializeClient() {
      ModuleManager.init();
      ConfigManager.load();
      openGuiKey = KeyBindingHelper.registerKeyBinding(new class_304("key.kafkautils.open_gui", class_307.field_1668, 88, class_11900.field_62556));
      hideHudKey = KeyBindingHelper.registerKeyBinding(new class_304("key.kafkautils.hide_hud", class_307.field_1668, 261, class_11900.field_62556));
      shareCoordsKey = KeyBindingHelper.registerKeyBinding(new class_304("key.kafkautils.share_coords", class_307.field_1668, -1, class_11900.field_62556));

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

      // Incoming: hide friend-chat whispers and show them nicely; then AntiSpam.
      ClientReceiveMessageEvents.ALLOW_GAME.register((ClientReceiveMessageEvents.AllowGame)(message, overlay) -> {
         FriendChat fc = (FriendChat)ModuleManager.get(FriendChat.class);
         if (fc != null && fc.handleIncoming(message.getString())) {
            return false;
         }
         AntiSpam as = (AntiSpam)ModuleManager.get(AntiSpam.class);
         return as == null || as.allow(message);
      });

      ClientReceiveMessageEvents.MODIFY_GAME.register((ClientReceiveMessageEvents.ModifyGame)(message, overlay) -> {
         if (overlay) {
            return message;
         }
         class_2561 result = message;
         ChatPing ping = (ChatPing)ModuleManager.get(ChatPing.class);
         if (ping != null) {
            result = ping.process(result);
         }
         FriendHighlight fh = (FriendHighlight)ModuleManager.get(FriendHighlight.class);
         if (fh != null) {
            result = fh.process(result);
         }
         return result;
      });

      // Outgoing: intercept "// message" (as chat and as command) for Friend Chat.
      ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
         FriendChat fc = (FriendChat)ModuleManager.get(FriendChat.class);
         return fc == null || !fc.handleChat(message);
      });
      ClientSendMessageEvents.ALLOW_COMMAND.register((command) -> {
         FriendChat fc = (FriendChat)ModuleManager.get(FriendChat.class);
         return fc == null || !fc.handleCommand(command);
      });

      // Client command: /kafka team add|remove|list , /kafka enchant
      ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
         dispatcher.register(ClientCommandManager.literal("kafka").then(ClientCommandManager.literal("team")
            .then(ClientCommandManager.literal("add").then(ClientCommandManager.argument("name", StringArgumentType.word()).executes(c -> {
               String name = StringArgumentType.getString(c, "name");
               FriendList fl = (FriendList)ModuleManager.get(FriendList.class);
               boolean ok = fl != null && fl.addFriend(name);
               c.getSource().sendFeedback(class_2561.method_43470(ok ? "§aДруг добавлен: §r" + name : "§7Уже в друзьях или ошибка"));
               return 1;
            })))
            .then(ClientCommandManager.literal("remove").then(ClientCommandManager.argument("name", StringArgumentType.word()).executes(c -> {
               String name = StringArgumentType.getString(c, "name");
               FriendList fl = (FriendList)ModuleManager.get(FriendList.class);
               boolean ok = fl != null && fl.removeFriend(name);
               c.getSource().sendFeedback(class_2561.method_43470(ok ? "§cДруг удалён: §r" + name : "§7Не найден"));
               return 1;
            })))
            .then(ClientCommandManager.literal("list").executes(c -> {
               FriendList fl = (FriendList)ModuleManager.get(FriendList.class);
               String list = fl == null || fl.friends().isEmpty() ? "§7пусто" : "§r" + String.join(", ", fl.friends());
               c.getSource().sendFeedback(class_2561.method_43470("§dДрузья: " + list));
               return 1;
            })))
            .then(ClientCommandManager.literal("enchant").executes(c -> {
               EnchantHelper eh = (EnchantHelper)ModuleManager.get(EnchantHelper.class);
               if (eh != null) {
                  eh.printHints();
               }
               return 1;
            }).then(ClientCommandManager.argument("preset", StringArgumentType.word()).suggests((ctx, b) -> {
               EnchantHelper eh = (EnchantHelper)ModuleManager.get(EnchantHelper.class);
               if (eh != null) {
                  for (String k : eh.presetKeys()) {
                     b.suggest(k, new LiteralMessage(eh.presetTooltip(k)));
                  }
               }
               return b.buildFuture();
            }).executes(c -> {
               EnchantHelper eh = (EnchantHelper)ModuleManager.get(EnchantHelper.class);
               if (eh != null) {
                  eh.printPreset(StringArgumentType.getString(c, "preset"));
               }
               return 1;
            }))));
      });

      ClientPlayConnectionEvents.JOIN.register((ClientPlayConnectionEvents.Join)(handler, sender, client) -> HudManager.onWorldJoin());
      System.out.println("[KafkaUtils] Initialized — open the menu with X (rebind in Options → Controls).");
   }
}
