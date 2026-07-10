package dev.kafka.kafkautils.setting;

public class StringSetting extends Setting {
   private String value;

   public StringSetting(String name, String defaultValue) {
      super(name);
      this.value = defaultValue == null ? "" : defaultValue;
   }

   public String get() {
      return this.value;
   }

   public void set(String v) {
      this.value = v == null ? "" : v;
   }

   public String serialize() {
      return this.value;
   }

   public void deserialize(String raw) {
      this.value = raw == null ? "" : raw;
   }
}
