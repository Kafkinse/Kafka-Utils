package dev.kafka.kafkautils.setting;

public class NumberSetting extends Setting {
   private int value;
   private final int min;
   private final int max;
   private final int step;

   public NumberSetting(String name, int defaultValue, int min, int max, int step) {
      super(name);
      this.min = min;
      this.max = max;
      this.step = step;
      this.value = this.clamp(defaultValue);
   }

   public int get() {
      return this.value;
   }

   public void set(int v) {
      this.value = this.clamp(v);
   }

   public void cycle() {
      int next = this.value + this.step;
      if (next > this.max) {
         next = this.min;
      }

      this.value = next;
   }

   public int min() {
      return this.min;
   }

   public int max() {
      return this.max;
   }

   private int clamp(int v) {
      return Math.max(this.min, Math.min(this.max, v));
   }

   public String serialize() {
      return Integer.toString(this.value);
   }

   public void deserialize(String raw) {
      try {
         this.set(Integer.parseInt(raw.trim()));
      } catch (Exception var3) {
      }

   }
}
