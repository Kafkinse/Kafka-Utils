package dev.kafka.kafkautils.setting;

import java.util.List;

public class ModeSetting extends Setting {
   private final List<String> modes;
   private int index;

   public ModeSetting(String name, int defaultIndex, String... modes) {
      super(name);
      this.modes = List.of(modes);
      this.index = defaultIndex >= 0 && defaultIndex < this.modes.size() ? defaultIndex : 0;
   }

   public String get() {
      return (String)this.modes.get(this.index);
   }

   public int index() {
      return this.index;
   }

   public void cycle() {
      this.index = (this.index + 1) % this.modes.size();
   }

   public String serialize() {
      return Integer.toString(this.index);
   }

   public void deserialize(String raw) {
      try {
         int i = Integer.parseInt(raw.trim());
         if (i >= 0 && i < this.modes.size()) {
            this.index = i;
         }
      } catch (Exception var3) {
      }

   }
}
