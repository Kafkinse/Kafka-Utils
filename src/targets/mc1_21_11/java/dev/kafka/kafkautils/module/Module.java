package dev.kafka.kafkautils.module;

import dev.kafka.kafkautils.setting.Setting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_310;

public abstract class Module {
   protected static final class_310 mc = class_310.method_1551();
   private final String name;
   private final String description;
   private final Category category;
   private final List<Setting> settings = new ArrayList();
   private boolean enabled;

   protected <T extends Setting> T add(T setting) {
      this.settings.add(setting);
      return setting;
   }

   public List<Setting> getSettings() {
      return this.settings;
   }

   protected Module(String name, String description, Category category) {
      this.name = name;
      this.description = description;
      this.category = category;
   }

   protected void onEnable() {
   }

   protected void onDisable() {
   }

   public void onTick() {
   }

   public void toggle() {
      this.setEnabled(!this.enabled);
   }

   public void setEnabled(boolean value) {
      if (value != this.enabled) {
         this.enabled = value;
         if (this.enabled) {
            this.onEnable();
         } else {
            this.onDisable();
         }

      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public Category getCategory() {
      return this.category;
   }
}
