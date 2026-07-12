package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.hud.HudManager;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class MaceAlert extends Module {
    public Setting<Integer> minHeight = this.register(new Setting<>("Min Height", 8, 3, 20));
    public Setting<Integer> detectionRange = this.register(new Setting<>("Detection Range", 20, 10, 40));
    public Setting<String> alertLevel = this.register(new Setting<>("Alert Level", "IMPACT", "WATCH", "DANGER", "IMPACT"));
    public Setting<Boolean> soundEnabled = this.register(new Setting<>("Sound", true));
    public Setting<Boolean> autoShield = this.register(new Setting<>("Auto Shield", false));
    public Setting<Boolean> showTimer = this.register(new Setting<>("Show Timer", true));
    public Setting<Boolean> showDamageEst = this.register(new Setting<>("Show Damage Est", true));

    private enum ThreatLevel {
        SAFE,
        WATCH,
        DANGER,
        IMPACT
    }

    private ThreatLevel currentThreat = ThreatLevel.SAFE;
    private PlayerEntity threatPlayer = null;
    private long threatStartTime = 0;
    private float estimatedDamage = 0;
    private Vec3d impactDirection = null;
    private int impactCountdown = 0;

    private static final int KAFKA_PURPLE = 0xFF9D4EDD; // Kafka HSR color
    private static final int WATCH_COLOR = 0xFFB88ED6;  // Light purple
    private static final int DANGER_COLOR = 0xFFA855D0; // Medium purple
    private static final int IMPACT_COLOR = 0xFF8A2BE2;  // Dark purple

    public MaceAlert() {
        super("Mace Alert", KAFKA_PURPLE);
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        threatPlayer = null;
        currentThreat = ThreatLevel.SAFE;
        estimatedDamage = 0;
        impactDirection = null;
        impactCountdown = 0;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.isSpectator()) continue;
            if (mc.player.distanceTo(player) > detectionRange.getValue()) continue;

            ThreatLevel level = analyzeThreat(player, mc.player);
            if (level.ordinal() > currentThreat.ordinal()) {
                currentThreat = level;
                threatPlayer = player;
                threatStartTime = System.currentTimeMillis();
                estimatedDamage = calculateDamage(player);
                impactDirection = getImpactDirection(player, mc.player);
            }
        }

        if (currentThreat != ThreatLevel.SAFE) {
            handleThreat(mc);
        } else {
            HudManager.removeHud("MaceAlertTimer");
        }
    }

    private ThreatLevel analyzeThreat(PlayerEntity attacker, PlayerEntity target) {
        // Check if player has mace
        if (!hasMace(attacker)) {
            return ThreatLevel.SAFE;
        }

        Vec3d velocity = attacker.getVelocity();
        double distance = target.distanceTo(attacker);

        // WATCH: Enemy gaining height with mace
        if (velocity.y > 0.15 && hasWeaponInHand(attacker)) {
            return ThreatLevel.WATCH;
        }

        // DANGER: Enemy falling from height
        if (velocity.y < -0.3 && attacker.getY() - target.getY() > minHeight.getValue()) {
            Vec3d toTarget = target.getPos().subtract(attacker.getPos()).normalize();
            if (toTarget.dot(velocity.normalize()) > 0.5) {
                return ThreatLevel.DANGER;
            }
        }

        // IMPACT: Enemy very close and falling
        if (distance < 10 && velocity.y < -0.5 && attacker.getY() > target.getY()) {
            impactCountdown = calculateImpactTime(attacker, target);
            return ThreatLevel.IMPACT;
        }

        return ThreatLevel.SAFE;
    }

    private boolean hasMace(PlayerEntity player) {
        return player.getMainHandStack().getItem() == Items.MACE ||
               player.getOffHandStack().getItem() == Items.MACE;
    }

    private boolean hasWeaponInHand(PlayerEntity player) {
        return player.getMainHandStack().getItem() == Items.MACE ||
               player.getMainHandStack().getItem() == Items.DIAMOND_SWORD ||
               player.getMainHandStack().getItem() == Items.NETHERITE_SWORD;
    }

    private float calculateDamage(PlayerEntity attacker) {
        float baseDamage = 9; // Mace base damage
        float velocity = (float) attacker.getVelocity().length();
        return baseDamage + (velocity * 5);
    }

    private Vec3d getImpactDirection(PlayerEntity attacker, PlayerEntity target) {
        return target.getPos().subtract(attacker.getPos()).normalize();
    }

    private int calculateImpactTime(PlayerEntity attacker, PlayerEntity target) {
        double distance = attacker.distanceTo(target);
        double velocity = Math.abs(attacker.getVelocity().y);
        if (velocity < 0.1) return 0;
        return (int) ((distance / velocity) * 20); // ticks
    }

    private void handleThreat(MinecraftClient mc) {
        if (soundEnabled.getValue()) {
            playThreatSound(mc);
        }

        if (autoShield.getValue() && currentThreat == ThreatLevel.IMPACT) {
            activateShield(mc);
        }

        if (showTimer.getValue() && currentThreat == ThreatLevel.IMPACT) {
            HudManager.addHud("MaceAlertTimer", (matrices, tickDelta) -> {
                String timer = String.format("IMPACT IN: %dms", impactCountdown * 50);
                // Render timer at center of screen
            });
        }

        // Screen border color change
        updateScreenBorder(mc);
    }

    private void playThreatSound(MinecraftClient mc) {
        float pitch = 1.0f + (currentThreat.ordinal() * 0.2f);
        switch (currentThreat) {
            case WATCH:
                mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, pitch * 0.8f);
                break;
            case DANGER:
                mc.player.playSound(SoundEvents.ENTITY_GUARDIAN_ATTACK, 0.6f, pitch * 0.9f);
                break;
            case IMPACT:
                mc.player.playSound(SoundEvents.BLOCK_ANVIL_LAND, 1.0f, pitch);
                break;
            default:
                break;
        }
    }

    private void activateShield(MinecraftClient mc) {
        if (mc.player != null && mc.options.useKey.isPressed() == false) {
            mc.player.getOffHandStack().getItem(); // Check for shield
            // Trigger right-click to raise shield
        }
    }

    private void updateScreenBorder(MinecraftClient mc) {
        // This would be implemented in rendering system
        // Color changes based on threat level
    }

    @Override
    public String getInfo() {
        if (threatPlayer != null) {
            return String.format("%s - %s", currentThreat.name(), String.format("%.1f", estimatedDamage) + " DMG");
        }
        return "Safe";
    }
}
