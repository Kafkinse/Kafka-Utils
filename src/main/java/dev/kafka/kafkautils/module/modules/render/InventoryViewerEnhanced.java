package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;

public class InventoryViewer extends Module {
    public Setting<Boolean> showHotbar = this.register(new Setting<>("Show Hotbar", true));
    public Setting<Boolean> showArmor = this.register(new Setting<>("Show Armor", true));
    public Setting<Boolean> showOffhand = this.register(new Setting<>("Show Offhand", true));
    public Setting<Integer> renderDistance = this.register(new Setting<>("Render Distance", 60, 20, 100));
    public Setting<Integer> posX = this.register(new Setting<>("Pos X", 10, 0, 1000));
    public Setting<Integer> posY = this.register(new Setting<>("Pos Y", 10, 0, 1000));
    public Setting<Boolean> scale = this.register(new Setting<>("Scale", true));

    private static final int KAFKA_PURPLE = 0xFF9D4EDD;
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_GAP = 2;

    private PlayerEntity trackedPlayer = null;

    public InventoryViewer() {
        super("Inventory Viewer", KAFKA_PURPLE);
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // Find nearest player
        trackedPlayer = null;
        double minDistance = renderDistance.getValue();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            double distance = mc.player.distanceTo(player);
            if (distance < minDistance) {
                minDistance = distance;
                trackedPlayer = player;
            }
        }
    }

    public void renderInventory(MatrixStack matrices, int mouseX, int mouseY) {
        if (trackedPlayer == null) return;

        int x = posX.getValue();
        int y = posY.getValue();

        if (showArmor.getValue()) {
            renderArmorSlots(matrices, x, y);
            y += (SLOT_SIZE + SLOT_GAP) * 4 + 5;
        }

        if (showOffhand.getValue()) {
            renderOffhandSlot(matrices, x, y);
            y += SLOT_SIZE + SLOT_GAP + 5;
        }

        if (showHotbar.getValue()) {
            renderHotbar(matrices, x, y);
        }
    }

    private void renderArmorSlots(MatrixStack matrices, int x, int y) {
        // Helmet, Chestplate, Leggings, Boots
        for (int i = 0; i < 4; i++) {
            ItemStack armor = trackedPlayer.getArmorItems().get(i);
            int slotY = y + (i * (SLOT_SIZE + SLOT_GAP));
            renderSlot(matrices, x, slotY, armor);
        }
    }

    private void renderOffhandSlot(MatrixStack matrices, int x, int y) {
        ItemStack offhand = trackedPlayer.getOffHandStack();
        renderSlot(matrices, x, y, offhand);
    }

    private void renderHotbar(MatrixStack matrices, int x, int y) {
        for (int i = 0; i < 9; i++) {
            ItemStack hotbarItem = trackedPlayer.getInventory().getStack(i);
            int slotX = x + (i * (SLOT_SIZE + SLOT_GAP));
            renderSlot(matrices, slotX, y, hotbarItem);
        }
    }

    private void renderSlot(MatrixStack matrices, int x, int y, ItemStack stack) {
        // Render slot background
        fill(matrices, x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF8B5A8B); // Kafka purple

        if (!stack.isEmpty()) {
            // Render item
            MinecraftClient mc = MinecraftClient.getInstance();
            // Item rendering would be done here

            // Render count if > 1
            if (stack.getCount() > 1) {
                String count = String.valueOf(stack.getCount());
                TextRenderer textRenderer = mc.textRenderer;
                int textX = x + SLOT_SIZE - 8 - textRenderer.getWidth(count);
                int textY = y + SLOT_SIZE - 8;
                textRenderer.draw(matrices, count, textX, textY, 0xFFFFFF);
            }
        }
    }

    private void fill(MatrixStack matrices, int x1, int y1, int x2, int y2, int color) {
        // Rectangle filling implementation
    }

    public PlayerEntity getTrackedPlayer() {
        return trackedPlayer;
    }

    @Override
    public String getInfo() {
        if (trackedPlayer != null) {
            return trackedPlayer.getName().getString();
        }
        return "No Target";
    }
}
