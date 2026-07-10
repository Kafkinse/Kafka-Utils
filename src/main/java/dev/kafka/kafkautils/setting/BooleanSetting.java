package dev.kafka.kafkautils.setting;

public class BooleanSetting extends Setting {
   private boolean value;

   public BooleanSetting(String name, boolean defaultValue) {
      super(name);
      this.value = defaultValue;
   }

   public boolean get() {
      return this.value;
   }

   public void set(boolean v) {
      this.value = v;
   }

   public void toggle() {
      this.value = !this.value;
   }

   public String serialize() {
      return Boolean.toString(this.value);
   }

   public void deserialize(String raw) {
      this.value = Boolean.parseBoolean(raw);
   }
}
