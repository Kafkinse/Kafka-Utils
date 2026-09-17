package dev.kafka.kafkautils.setting;

public abstract class Setting {
   private final String name;

   protected Setting(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public String getKey() {
      return this.name.toLowerCase().replace(' ', '_');
   }

   public abstract String serialize();

   public abstract void deserialize(String var1);
}
