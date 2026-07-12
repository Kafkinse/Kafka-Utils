package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.Setting;
import net.minecraft.client.MinecraftClient;

public class TrailerModule extends Module {
    // This module serves as a placeholder/trailer for the mod
    // It's designed to be unremarkable in the module list
    
    private static final int KAFKA_PURPLE = 0xFF9D4EDD;

    public TrailerModule() {
        super("Utility Utils", KAFKA_PURPLE);
    }

    @Override
    public void tick() {
        // Minimal functionality
    }

    @Override
    public String getInfo() {
        return "v1.0";
    }
}
