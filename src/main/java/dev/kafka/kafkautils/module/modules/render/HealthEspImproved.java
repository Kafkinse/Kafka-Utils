package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class HealthEspImproved extends Module {
    public Setting<Boolean> showThroughWalls = this.register(new Setting<>("Through Walls", true));
    public Setting<Boolean> showArmorBar = this.register(new Setting<>("Show Armor", true));
    public Setting<Integer> barHeight = this.register(new Setting<>("Bar Height", 3, 1, 8));
    public Setting<Integer> renderDistance = this.register(new Setting<>("Render Distance", 60, 10, 150));
    public Setting<Boolean> facingPlayer = this.register(new Setting<>("Face Player", true));

    private static final int KAFKA_PURPLE = 0xFF9D4EDD;
    private static final int HEALTH_COLOR = 0xFF00FF00;  // Green
    private static final int ARMOR_COLOR = 0xFFB0C4DE;   // Light steel blue
    private static final int BACKGROUND = 0xFF1A1A1A;    // Dark background

    public HealthEspImproved() {
        super("Health ESP+", KAFKA_PURPLE);
    }

    @Override
    public void tick() {
        // Handled in render event
    }

    public void renderHealthBar(LivingEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !(entity instanceof PlayerEntity)) return;
        if (mc.player.distanceTo(entity) > renderDistance.getValue()) return;

        PlayerEntity player = (PlayerEntity) entity;
        Vec3d headPos = entity.getPos().add(0, entity.getHeight() + 0.3, 0);

        matrices.push();

        // Face the player
        if (facingPlayer.getValue()) {
            faceCameraInverse(matrices, mc, headPos);
        }

        // Health bar background
        float barWidth = 32;
        float barHeight = this.barHeight.getValue();
        drawBar(matrices, -barWidth / 2, 0, barWidth / 2, barHeight, BACKGROUND);

        // Health bar fill
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float healthPercent = health / maxHealth;
        float fillWidth = (barWidth - 2) * healthPercent;
        int healthColor = getHealthColor(healthPercent);
        drawBar(matrices, -barWidth / 2 + 1, 1, -barWidth / 2 + 1 + fillWidth, barHeight - 1, healthColor);

        // Armor bar (below health)
        if (showArmorBar.getValue()) {
            float armor = player.getArmor();
            float maxArmor = 20;
            float armorPercent = armor / maxArmor;
            float armorFillWidth = (barWidth - 2) * armorPercent;

            drawBar(matrices, -barWidth / 2, barHeight + 1, barWidth / 2, barHeight + 1 + barHeight, BACKGROUND);
            drawBar(matrices, -barWidth / 2 + 1, barHeight + 2, -barWidth / 2 + 1 + armorFillWidth, barHeight + barHeight - 1, ARMOR_COLOR);
        }

        // Player name
        String playerName = player.getName().getString();
        int nameColor = KAFKA_PURPLE;
        mc.textRenderer.draw(matrices, playerName, -mc.textRenderer.getWidth(playerName) / 2, -8, nameColor);

        matrices.pop();
    }

    private void faceCameraInverse(MatrixStack matrices, MinecraftClient mc, Vec3d pos) {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Vec3d diff = pos.subtract(cameraPos);
        
        double pitch = Math.atan2(-diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z));
        double yaw = Math.atan2(diff.x, diff.z);

        matrices.multiply(com.mojang.math.Axis.YP.rotationDegrees((float) Math.toDegrees(-yaw)));
        matrices.multiply(com.mojang.math.Axis.XP.rotationDegrees((float) Math.toDegrees(-pitch)));
    }

    private void drawBar(MatrixStack matrices, float x1, float y1, float x2, float y2, int color) {
        float r = (color >> 16 & 0xFF) / 255.0f;
        float g = (color >> 8 & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = (color >> 24 & 0xFF) / 255.0f;

        // Would use actual rendering here
    }

    private int getHealthColor(float healthPercent) {
        if (healthPercent > 0.75f) {
            return 0xFF00FF00; // Green
        } else if (healthPercent > 0.5f) {
            return 0xFFFFFF00; // Yellow
        } else if (healthPercent > 0.25f) {
            return 0xFFFF8800; // Orange
        } else {
            return 0xFFFF0000; // Red
        }
    }

    @Override
    public String getInfo() {
        return "Active";
    }
}
