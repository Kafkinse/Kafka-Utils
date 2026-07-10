package dev.kafka.kafkautils.module;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

public interface WorldRenderModule {
   void onWorldRender(WorldRenderContext var1);
}
