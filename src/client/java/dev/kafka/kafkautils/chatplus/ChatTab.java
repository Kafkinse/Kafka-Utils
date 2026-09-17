package dev.kafka.kafkautils.chatplus;

/** Built-in chat tabs shown in the F8 chat manager. */
public enum ChatTab {
   GLOBAL("Общий"),
   SYSTEM("Система");

   private final String label;

   ChatTab(String label) {
      this.label = label;
   }

   public String label() {
      return this.label;
   }
}
