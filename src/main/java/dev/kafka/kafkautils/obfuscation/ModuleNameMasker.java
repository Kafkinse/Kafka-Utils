package dev.kafka.kafkautils.obfuscation;

import net.minecraft.util.Identifier;

/**
 * Module name obfuscation utility
 * Maps actual module names to innocent-looking names
 */
public class ModuleNameMasker {
    
    // Maps actual dangerous sounding names to innocent ones
    public static String maskModuleName(String actualName) {
        return switch (actualName) {
            case "MaceAlert" -> "Tool Assistant";
            case "ArmorDurabilityEsp" -> "Equipment Monitor";
            case "PearlTrackerEnhanced" -> "Projectile Helper";
            case "InventorySorter" -> "Bag Manager";
            case "InventoryViewerEnhanced" -> "Container Viewer";
            case "DpsMeter" -> "Performance Tracker";
            case "KillDeathTracker" -> "Session Stats";
            case "ArmorAlertModule" -> "Durability Alert";
            case "HealthEspImproved" -> "Entity Monitor";
            default -> actualName;
        };
    }
    
    /**
     * Get display name for GUI
     */
    public static String getDisplayName(String moduleName) {
        return maskModuleName(moduleName);
    }
    
    /**
     * Get category name (also masked)
     */
    public static String getCategoryName(String category) {
        return switch (category) {
            case "combat" -> "Utilities";
            case "render" -> "Visual";
            case "chat" -> "Social";
            default -> category;
        };
    }
}
