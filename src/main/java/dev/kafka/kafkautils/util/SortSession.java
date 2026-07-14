package dev.kafka.kafkautils.util;

import net.minecraft.class_1703;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.minecraft.class_636;
import net.minecraft.class_746;
import net.minecraft.class_7923;

/**
 * Sorts a set of slots inside a screen handler by driving the vanilla interaction
 * manager. It runs one selection-sort step per {@link #step} call (a single
 * three-click swap), so a module can spread the work across ticks and stop if the
 * screen is closed. Items are ordered by registry id, then by stack size; empty
 * slots sink to the end. Equal items get consolidated by the game's own stacking.
 */
public final class SortSession {
   private final class_1703 handler;
   private final int[] slotIds;
   private int cursor;

   public SortSession(class_1703 handler, int[] slotIds) {
      this.handler = handler;
      this.slotIds = slotIds;
      this.cursor = 0;
   }

   public boolean isDone() {
      return this.cursor >= this.slotIds.length;
   }

   public int total() {
      return this.slotIds.length;
   }

   public int progress() {
      return this.cursor;
   }

   public void step(class_636 interaction, class_746 player) {
      if (this.isDone()) {
         return;
      }
      int min = this.cursor;
      for (int k = this.cursor + 1; k < this.slotIds.length; ++k) {
         if (less(this.stackAt(k), this.stackAt(min))) {
            min = k;
         }
      }
      if (min != this.cursor) {
         int syncId = this.handler.field_7763;
         int a = this.slotIds[this.cursor];
         int b = this.slotIds[min];
         interaction.method_2906(syncId, b, 0, class_1713.field_7790, player); // pick up b
         interaction.method_2906(syncId, a, 0, class_1713.field_7790, player); // drop b into a, pick up a
         interaction.method_2906(syncId, b, 0, class_1713.field_7790, player); // drop a into b
      }
      ++this.cursor;
   }

   private class_1799 stackAt(int k) {
      return this.handler.field_7761.get(this.slotIds[k]).method_7677();
   }

   private static boolean less(class_1799 a, class_1799 b) {
      boolean ea = a.method_7960();
      boolean eb = b.method_7960();
      if (ea != eb) {
         return eb; // a non-empty comes before empty b
      }
      if (ea) {
         return false;
      }
      int c = id(a).compareTo(id(b));
      if (c != 0) {
         return c < 0;
      }
      return a.method_7947() > b.method_7947();
   }

   private static String id(class_1799 stack) {
      return class_7923.field_41178.method_10221(stack.method_7909()).method_12832();
   }
}
