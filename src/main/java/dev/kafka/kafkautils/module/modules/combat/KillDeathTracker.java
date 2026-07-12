package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import java.util.*;

public class KillDeathTracker extends Module {
    public Setting<Boolean> resetOnWorldChange = this.register(new Setting<>("Reset On World Change", false));
    public Setting<Boolean> showDeathMessages = this.register(new Setting<>("Show Death Messages", true));

    private static final int KAFKA_PURPLE = 0xFF9D4EDD;

    private int kills = 0;
    private int deaths = 0;
    private Map<String, Integer> playerKills = new HashMap<>();
    private String lastWorld = "";

    public KillDeathTracker() {
        super("Kill/Death Tracker", KAFKA_PURPLE);
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        String currentWorld = mc.world.getDimension().toString();
        if (!lastWorld.equals(currentWorld) && resetOnWorldChange.getValue()) {
            resetStats();
            lastWorld = currentWorld;
        }
    }

    public void recordKill(PlayerEntity victim) {
        kills++;
        playerKills.put(victim.getName().getString(), 
            playerKills.getOrDefault(victim.getName().getString(), 0) + 1);
    }

    public void recordDeath(PlayerEntity killer) {
        deaths++;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public double getKdr() {
        if (deaths == 0) return kills > 0 ? kills : 0;
        return (double) kills / deaths;
    }

    public Map<String, Integer> getPlayerKills() {
        return new HashMap<>(playerKills);
    }

    public void resetStats() {
        kills = 0;
        deaths = 0;
        playerKills.clear();
    }

    @Override
    public String getInfo() {
        return String.format("K:%d D:%d KDR:%.2f", kills, deaths, getKdr());
    }
}
