package dev.kafka.kafkautils.util;

import net.minecraft.util.math.MathHelper;

/**
 * Utility class for color management with Kafka HSR theme
 * Uses fioletchrome purple color scheme
 */
public class KafkaColorTheme {
    // Primary Kafka colors
    public static final int PRIMARY_PURPLE = 0xFF9D4EDD;      // Main purple
    public static final int SECONDARY_PURPLE = 0xFFB88ED6;    // Light purple
    public static final int DARK_PURPLE = 0xFF8A2BE2;         // Dark purple
    public static final int ACCENT_PURPLE = 0xFFA855D0;       // Accent purple
    
    // Alert colors
    public static final int WATCH_COLOR = 0xFFB88ED6;         // Light purple - WATCH
    public static final int DANGER_COLOR = 0xFFA855D0;        // Medium purple - DANGER
    public static final int IMPACT_COLOR = 0xFF8A2BE2;        // Dark purple - IMPACT
    
    // Health/Status colors
    public static final int HEALTH_CRITICAL = 0xFFFF0000;     // Red
    public static final int HEALTH_LOW = 0xFFFF8800;          // Orange
    public static final int HEALTH_MEDIUM = 0xFFFFFF00;       // Yellow
    public static final int HEALTH_HIGH = 0xFF00FF00;         // Green
    
    // Armor colors
    public static final int ARMOR_COLOR = 0xFFB0C4DE;         // Light steel blue
    public static final int DURABILITY_GOOD = 0xFF00FF00;     // Green
    public static final int DURABILITY_WARNING = 0xFFFFFF00;  // Yellow
    public static final int DURABILITY_CRITICAL = 0xFFFF0000; // Red
    
    /**
     * Blend two colors together
     */
    public static int blend(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int) MathHelper.lerp(ratio, a1, a2);
        int r = (int) MathHelper.lerp(ratio, r1, r2);
        int g = (int) MathHelper.lerp(ratio, g1, g2);
        int b = (int) MathHelper.lerp(ratio, b1, b2);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Get health bar color based on percentage
     */
    public static int getHealthBarColor(float healthPercent) {
        if (healthPercent > 0.75f) {
            return HEALTH_HIGH;
        } else if (healthPercent > 0.5f) {
            return HEALTH_MEDIUM;
        } else if (healthPercent > 0.25f) {
            return HEALTH_LOW;
        } else {
            return HEALTH_CRITICAL;
        }
    }
    
    /**
     * Get durability bar color based on percentage
     */
    public static int getDurabilityColor(float durabilityPercent) {
        if (durabilityPercent > 50) {
            return DURABILITY_GOOD;
        } else if (durabilityPercent > 25) {
            return DURABILITY_WARNING;
        } else {
            return DURABILITY_CRITICAL;
        }
    }
    
    /**
     * Adjust alpha of a color
     */
    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }
}
