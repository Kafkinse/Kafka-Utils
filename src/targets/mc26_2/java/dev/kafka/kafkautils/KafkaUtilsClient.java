package dev.kafka.kafkautils;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kafka.kafkautils.chatplus.ChatPlusBootstrap;
import dev.kafka.kafkautils.chatplus.ChatPlusScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
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
      ChatPlusBootstrap.init();

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
      });

      LOGGER.info("Kafka Utils (26.2 target) loaded");
   }
}
