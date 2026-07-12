package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ArmorDurabilityEsp extends Module {
    public Setting<Boolean> showAboveHead = this.register(new Setting<>("Show Above Head", true));
    public Setting<Integer> renderDistance = this.register(new Setting<>("Render Distance", 50, 10, 100));
    public Setting<Boolean> onlyLowDurability = this.register(new Setting<>("Only Low Durability", false));
    public Setting<Integer> durabilityThreshold = this.register(new Setting<>("Durability Threshold", 50, 1, 100));

    private static final int KAFKA_PURPLE = 0xFF9D4EDD;
    private static final int DURABLE_COLOR = 0xFF00FF00;  // Green
    private static final int MEDIUM_COLOR = 0xFFFFFF00;   // Yellow
    private static final int LOW_COLOR = 0xFFFF0000;       // Red

    public ArmorDurabilityEsp() {
        super("Armor Durability ESP", KAFKA_PURPLE);
    }

    @Override
    public void tick() {
        // This will be handled in render event
    }

    public void renderArmorDurability(LivingEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !(entity instanceof PlayerEntity)) return;
        if (mc.player.distanceTo(entity) > renderDistance.getValue()) return;

        PlayerEntity player = (PlayerEntity) entity;
        StringBuilder armorInfo = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            ItemStack armorStack = player.getArmorItems().get(i);
            if (!armorStack.isEmpty() && armorStack.getItem() instanceof ArmorItem) {
                float durability = getDurabilityPercent(armorStack);

                if (onlyLowDurability.getValue() && durability > durabilityThreshold.getValue()) {
                    continue;
                }

                String armorName = getArmorName(i);
                int color = getDurabilityColor(durability);
                armorInfo.append(String.format("%s: %.0f%% ", armorName, durability));
            }
        }

        if (armorInfo.length() > 0) {
            // Render above head
            Vec3d headPos = entity.getPos().add(0, entity.getHeight() + 0.5, 0);
            renderText3D(mc, matrices, vertexConsumers, light, headPos, armorInfo.toString(), KAFKA_PURPLE);
        }
    }

    private float getDurabilityPercent(ItemStack stack) {
        if (stack.isDamageable()) {
            return ((float) (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage()) * 100;
        }
        return 100;
    }

    private String getArmorName(int slot) {
        switch (slot) {
            case 0: return "B";
            case 1: return "L";
            case 2: return "C";
            case 3: return "H";
            default: return "?";
        }
    }

    private int getDurabilityColor(float durability) {
        if (durability > 50) return DURABLE_COLOR;
        if (durability > 25) return MEDIUM_COLOR;
        return LOW_COLOR;
    }

    private void renderText3D(MinecraftClient mc, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Vec3d pos, String text, int color) {
        // Implementation for 3D text rendering
        TextRenderer textRenderer = mc.textRenderer;
        // Position and render text
    }

    @Override
    public String getInfo() {
        return "Active";
    }
}
