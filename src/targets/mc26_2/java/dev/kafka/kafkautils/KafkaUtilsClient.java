package dev.kafka.kafkautils;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kafka.kafkautils.chatplus.ChatAlertHud;
import dev.kafka.kafkautils.chatplus.ChatAlertHudRegistration;
import dev.kafka.kafkautils.chatplus.ChatPlusBootstrap;
import dev.kafka.kafkautils.chatplus.ChatPlusScreen;
import dev.kafka.kafkautils.chatplus.VnbxBridgeNetworking;
import dev.kafka.kafkautils.chatplus.VnbxRelations;
import dev.kafka.kafkautils.util.UpdateChecker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the 26.2 target. F8 opens the cross-version chat manager
 * (see the {@code dev.kafka.kafkautils.chatplus} package); the existing
 * combat/render/economy modules stay 1.21.11-only until they get their own
 * 26.2 port.
 */
public final class KafkaUtilsClient implements ClientModInitializer {
   public static final String MOD_ID = "kafkautils";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   @Override
   public void onInitializeClient() {
      ChatPlusBootstrap.init(() -> Minecraft.getInstance().getUser().getName());
      ChatAlertHudRegistration.register();
      VnbxBridgeNetworking.register();
      UpdateChecker.start();

      KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "main"));
      KeyMapping chatPlusKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.kafkautils.chat_plus",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F8,
            category));

      ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
         while (chatPlusKey.consumeClick()) {
            minecraft.gui.setScreen(new ChatPlusScreen());
         }
         ChatAlertHud.tick();
         int chatAlertSounds = ChatAlertHud.drainPendingSounds();
         for (int i = 0; i < chatAlertSounds; ++i) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F, 0.75F));
         }
         if (minecraft.getConnection() != null) {
            UpdateChecker.consumeAvailable().ifPresent(
                  message -> minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal(message)));
         }
      });

      // Self-lookup on join is a deliberate smoke test: whether vanilla-box.ru's
      // server actually answers the vnbx:bridge channel at all is unverified
      // from here, so this prints one line either way — profile info if the
      // bridge responded, an explicit "недоступен" if it didn't — instead of
      // building a whole profile screen around an unconfirmed server feature.
      ClientPlayConnectionEvents.JOIN.register((handler, sender, minecraft) -> {
         VnbxBridgeNetworking.connected();
         String myName = minecraft.getUser().getName();
         VnbxBridgeNetworking.requestPlayerRelations(myName).thenAccept(relations ->
               minecraft.execute(() -> reportRelations(minecraft, relations)));
      });
      ClientPlayConnectionEvents.DISCONNECT.register((handler, minecraft) -> VnbxBridgeNetworking.disconnected());

      LOGGER.info("Kafka Utils (26.2 target) loaded");
   }

   private static void reportRelations(Minecraft minecraft, VnbxRelations relations) {
      String message;
      if (!relations.available()) {
         message = "§d[Kafka-Utils] §7vnbx:bridge недоступен на этом сервере — профиль через бридж не работает.";
      } else {
         String clan = relations.inClan()
               ? "[" + relations.clanTag() + "] " + relations.clanName() + " (" + relations.clanRank() + ")"
               : "не в клане";
         String marriage = relations.married() ? "женат/замужем за " + relations.partnerName() : "не в браке";
         message = "§d[Kafka-Utils] §7Профиль через bridge: §fклан §7— §f" + clan + " §7| брак §7— §f" + marriage;
      }
      minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal(message));
   }
}
