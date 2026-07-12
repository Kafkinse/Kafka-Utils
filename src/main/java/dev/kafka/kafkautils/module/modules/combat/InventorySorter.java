package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import java.util.*;

public class InventorySorter extends Module {
    public Setting<Boolean> enablePresets = this.register(new Setting<>("Enable Presets", true));
    public Setting<Boolean> showMissingItems = this.register(new Setting<>("Show Missing Items", true));
    public Setting<Integer> presetSlot = this.register(new Setting<>("Preset Slot", 1, 1, 9));

    private static final int KAFKA_PURPLE = 0xFF9D4EDD;

    private static class InventoryPreset {
        String name;
        Map<Integer, ItemStack> preset = new HashMap<>();
        long createdTime;

        InventoryPreset(String name) {
            this.name = name;
            this.createdTime = System.currentTimeMillis();
        }
    }

    private Map<String, InventoryPreset> presets = new LinkedHashMap<>();
    private String activePreset = null;
    private List<ItemStack> missingItems = new ArrayList<>();

    public InventorySorter() {
        super("Inventory Sort", KAFKA_PURPLE);
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (activePreset != null && showMissingItems.getValue()) {
            checkMissingItems(mc.player);
        }
    }

    public void savePreset(String name, PlayerEntity player) {
        if (!enablePresets.getValue()) return;

        InventoryPreset preset = new InventoryPreset(name);
        Inventory inventory = player.getInventory();

        // Save hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                preset.preset.put(i, stack.copy());
            }
        }

        presets.put(name, preset);
    }

    public void loadPreset(String name, PlayerEntity player) {
        if (!presets.containsKey(name)) return;

        InventoryPreset preset = presets.get(name);
        Inventory inventory = player.getInventory();
        activePreset = name;

        // This would be implemented with packet sending
        // to avoid cheating detection
    }

    private void checkMissingItems(PlayerEntity player) {
        if (activePreset == null || !presets.containsKey(activePreset)) return;

        missingItems.clear();
        InventoryPreset preset = presets.get(activePreset);
        Inventory inventory = player.getInventory();

        for (Map.Entry<Integer, ItemStack> entry : preset.preset.entrySet()) {
            ItemStack presetItem = entry.getValue();
            ItemStack actualItem = inventory.getStack(entry.getKey());

            if (!ItemStack.canCombine(presetItem, actualItem) || actualItem.getCount() < presetItem.getCount()) {
                missingItems.add(presetItem);
            }
        }
    }

    public List<ItemStack> getMissingItems() {
        return new ArrayList<>(missingItems);
    }

    public Map<String, InventoryPreset> getPresets() {
        return new HashMap<>(presets);
    }

    @Override
    public String getInfo() {
        if (activePreset != null) {
            return activePreset + " [" + missingItems.size() + " missing]";
        }
        return "Presets: " + presets.size();
    }
}
