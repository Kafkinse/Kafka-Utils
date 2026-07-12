package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.EnderpearlEntity;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;

/**
 * Pearl Throw Listener
 * Detects when players throw ender pearls
 */
public class PearlThrowListener {
    
    private static final PearlTrackerEnhanced tracker = new PearlTrackerEnhanced();
    
    /**
     * Called when an EnderpearL entity is spawned
     */
    public static void onPearlSpawn(EnderpearlEntity pearl) {
        // This would be called from a packet listener
        if (pearl.getOwner() instanceof net.minecraft.entity.player.PlayerEntity player) {
            tracker.recordPearlThrow(player, pearl.getPos());
        }
    }
    
    /**
     * Get the tracker instance
     */
    public static PearlTrackerEnhanced getTracker() {
        return tracker;
    }
}
