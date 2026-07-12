package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

public class ArmorAlert extends Module {
    public Setting<Integer> durabilityThreshold = this.register(new Setting<>("Threshold (%)", 25, 5, 75));
    public Setting<Boolean> soundAlert = this.register(new Setting<>("Sound Alert", true));
    public Setting<Integer> renderDistance = this.register(new Setting<>("Render Distance", 50, 10, 100));

    private static final int KAFKA_PURPLE = 0xFF9D4EDD;
    private static final int ALERT_COLOR = 0xFFFF0000; // Red

    public ArmorAlert() {
        super("Armor Alert", KAFKA_PURPLE);
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.isSpectator()) continue;
            if (mc.player.distanceTo(player) > renderDistance.getValue()) continue;

            checkArmorDurability(player, mc);
        }
    }

    private void checkArmorDurability(PlayerEntity player, MinecraftClient mc) {
        for (int i = 0; i < 4; i++) {
            var armor = player.getArmorItems().get(i);
            if (!armor.isEmpty() && armor.isDamageable()) {
                float durability = ((float) (armor.getMaxDamage() - armor.getDamage()) / armor.getMaxDamage()) * 100;

                if (durability <= durabilityThreshold.getValue()) {
                    if (soundAlert.getValue()) {
                        mc.player.playSound(
                            net.minecraft.sound.SoundEvents.BLOCK_ANVIL_LAND,
                            0.7f,
                            0.8f + (durability / 100f)
                        );
                    }
                }
            }
        }
    }

    @Override
    public String getInfo() {
        return "Monitoring";
    }
}
