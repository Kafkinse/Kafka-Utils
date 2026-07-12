package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import java.util.*;

public class DpsMeter extends Module {
    public Setting<Boolean> trackAllPlayers = this.register(new Setting<>("Track All Players", true));
    public Setting<Integer> trackingWindow = this.register(new Setting<>("Window (seconds)", 10, 5, 30));
    public Setting<Integer> renderDistance = this.register(new Setting<>("Render Distance", 50, 10, 100));

    private static final int KAFKA_PURPLE = 0xFF9D4EDD;

    private static class DamageRecord {
        float damage;
        long timestamp;

        DamageRecord(float dmg, long time) {
            this.damage = dmg;
            this.timestamp = time;
        }
    }

    private Map<String, List<DamageRecord>> playerDamage = new HashMap<>();
    private LivingEntity trackedTarget = null;

    public DpsMeter() {
        super("DPS Meter", KAFKA_PURPLE);
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // Find nearest enemy as target
        if (trackedTarget == null || trackedTarget.isDead()) {
            findNearestTarget(mc);
        }

        // Clean up old records
        cleanupOldRecords();
    }

    private void findNearestTarget(MinecraftClient mc) {
        double minDistance = renderDistance.getValue();
        LivingEntity closest = null;

        for (LivingEntity entity : mc.world.getEntities().stream()
                .filter(e -> e instanceof LivingEntity && e != mc.player)
                .map(e -> (LivingEntity) e)
                .toList()) {
            double distance = mc.player.distanceTo(entity);
            if (distance < minDistance) {
                minDistance = distance;
                closest = entity;
            }
        }
        trackedTarget = closest;
    }

    public void recordDamage(PlayerEntity dealer, LivingEntity target, float damage) {
        if (!trackAllPlayers.getValue() && target != trackedTarget) return;

        String playerName = dealer.getName().getString();
        playerDamage.computeIfAbsent(playerName, k -> new ArrayList<>())
                .add(new DamageRecord(damage, System.currentTimeMillis()));
    }

    public float getDps(String playerName) {
        if (!playerDamage.containsKey(playerName)) return 0;

        List<DamageRecord> records = playerDamage.get(playerName);
        long now = System.currentTimeMillis();
        long windowMs = trackingWindow.getValue() * 1000L;

        float totalDamage = records.stream()
                .filter(r -> now - r.timestamp <= windowMs)
                .map(r -> r.damage)
                .reduce(0f, Float::sum);

        return totalDamage / trackingWindow.getValue();
    }

    public float getDps(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            return getDps(((PlayerEntity) entity).getName().getString());
        }
        return 0;
    }

    private void cleanupOldRecords() {
        long now = System.currentTimeMillis();
        long windowMs = trackingWindow.getValue() * 1000L;

        playerDamage.forEach((player, records) -> {
            records.removeIf(r -> now - r.timestamp > windowMs);
        });

        playerDamage.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    @Override
    public String getInfo() {
        if (trackedTarget instanceof PlayerEntity) {
            float dps = getDps((PlayerEntity) trackedTarget);
            return String.format("%.1f DPS", dps);
        }
        return "No Target";
    }
}
