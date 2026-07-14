package dev.kafka.kafkautils.module;

public enum Category {
   COMBAT("Combat & Raids"),
   RENDER("Render & World"),
   CHAT("Chat & Monitoring"),
   CLAN("Clan");

   private final String title;

   private Category(String title) {
      this.title = title;
   }

   public String getTitle() {
      return this.title;
   }

   // $FF: synthetic method
   private static Category[] $values() {
      return new Category[]{COMBAT, RENDER, CHAT, CLAN};
   }
}
