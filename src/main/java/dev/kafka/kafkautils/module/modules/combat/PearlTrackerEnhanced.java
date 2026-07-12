package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.EndCrystalEntity;
import java.util.*;

public class PearlTrackerEnhanced extends Module {
    public Setting<Boolean> showCooldown = this.register(new Setting<>("Show Cooldown", true));
    public Setting<Integer> trackingDistance = this.register(new Setting<>("Tracking Distance", 60, 20, 100));

    private static final int KAFKA_PURPLE = 0xFF9D4EDD;
    private static final long PEARL_COOLDOWN = 15000; // 15 seconds in ms

    private static class PearlRecord {
        String playerName;
        long throwTime;
        Vec3d throwPosition;

        PearlRecord(String name, long time, Vec3d pos) {
            this.playerName = name;
            this.throwTime = time;
            this.throwPosition = pos;
        }
    }

    private Map<String, Long> pearlCooldowns = new HashMap<>();
    private List<PearlRecord> pearlHistory = new ArrayList<>();

    public PearlTrackerEnhanced() {
        super("Pearl Tracker", KAFKA_PURPLE);
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // Track pearl throws
        trackPearls(mc);
        updateCooldowns();
    }

    private void trackPearls(MinecraftClient mc) {
        // This would be implemented in a packet listener
        // When EnderpearlEntity is spawned:
        // 1. Find the player who threw it
        // 2. Record throw time and position
        // 3. Add cooldown to that player
    }

    private void updateCooldowns() {
        long currentTime = System.currentTimeMillis();
        pearlCooldowns.entrySet().removeIf(entry -> currentTime - entry.getValue() > PEARL_COOLDOWN);
    }

    public void recordPearlThrow(PlayerEntity thrower, Vec3d position) {
        pearlCooldowns.put(thrower.getName().getString(), System.currentTimeMillis());
        pearlHistory.add(new PearlRecord(thrower.getName().getString(), System.currentTimeMillis(), position));
    }

    public long getPearlCooldown(PlayerEntity player) {
        String name = player.getName().getString();
        if (!pearlCooldowns.containsKey(name)) return 0;
        long elapsed = System.currentTimeMillis() - pearlCooldowns.get(name);
        return Math.max(0, PEARL_COOLDOWN - elapsed);
    }

    public boolean canThrowPearl(PlayerEntity player) {
        return getPearlCooldown(player) == 0;
    }

    @Override
    public String getInfo() {
        return "Tracking: " + pearlCooldowns.size();
    }
}
