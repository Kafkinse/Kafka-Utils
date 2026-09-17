package dev.kafka.kafkautils;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the 26.2 target. Deliberately minimal for now — the
 * cross-version chat features (tabs, history, alerts, bookmarks, commands)
 * land here incrementally; the existing combat/render/economy modules stay
 * 1.21.11-only until they get their own 26.2 port.
 */
public final class KafkaUtilsClient implements ClientModInitializer {
   public static final String MOD_ID = "kafkautils";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   @Override
   public void onInitializeClient() {
      LOGGER.info("Kafka Utils (26.2 target) loaded");
   }
}
