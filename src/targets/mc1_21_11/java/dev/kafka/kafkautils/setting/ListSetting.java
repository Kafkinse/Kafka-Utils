package dev.kafka.kafkautils.setting;

import java.util.ArrayList;
import java.util.List;

public class ListSetting extends Setting {
   private final List<String> values = new ArrayList();

   public ListSetting(String name) {
      super(name);
   }

   public List<String> values() {
      return this.values;
   }

   public void addEntry(String v) {
      this.values.add(v == null ? "" : v);
   }

   public void setEntry(int i, String v) {
      if (i >= 0 && i < this.values.size()) {
         this.values.set(i, v == null ? "" : v);
      }

   }

   public void removeEntry(int i) {
      if (i >= 0 && i < this.values.size()) {
         this.values.remove(i);
      }

   }

   public String serialize() {
      return String.join("\n", this.values);
   }

   public void deserialize(String raw) {
      this.values.clear();
      if (raw != null && !raw.isEmpty()) {
         for(String s : raw.split("\n")) {
            this.values.add(s);
         }
      }

   }
}
