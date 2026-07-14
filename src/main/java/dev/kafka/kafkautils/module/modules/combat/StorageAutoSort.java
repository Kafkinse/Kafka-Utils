package dev.kafka.kafkautils.module.modules.combat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.util.RenderUtil;
import dev.kafka.kafkautils.util.SortSession;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1661;
import net.minecraft.class_1703;
import net.minecraft.class_1735;
import net.minecraft.class_332;
import net.minecraft.class_465;

/**
 * Sorts the contents of any open chest / barrel / shulker. With "Auto on Open"
 * it fires the moment you open a container; you can also re-sort at any time with
 * the "Sort" keybind (Options → Controls). Only the container's own slots are
 * touched, never your inventory or hotbar.
 */
public class StorageAutoSort extends Module implements HudModule {
   private final BooleanSetting autoOnOpen = this.add(new BooleanSetting("Auto on Open", true));
   private final NumberSetting delay = this.add(new NumberSetting("Delay", 2, 1, 20, 1));

   private SortSession session;
   private boolean wasOpen;
   private int tick;
   private String status = "—";

   public StorageAutoSort() {
      super("Storage AutoSort", "Sorts an open chest/container (auto or on the Sort key).", Category.FARMING);
   }

   protected void onEnable() {
      this.session = null;
      this.wasOpen = false;
      this.status = "—";
   }

   public void onTick() {
      if (mc.field_1724 == null || mc.field_1761 == null) {
         return;
      }
      boolean open = this.isContainerOpen();
      if (this.autoOnOpen.get() && open && !this.wasOpen) {
         this.sortNow();
      }
      this.wasOpen = open;

      if (this.session != null) {
         if (!open || this.session.isDone()) {
            this.status = this.session != null && this.session.isDone() ? "готово" : "—";
            this.session = null;
            return;
         }
         if (++this.tick >= this.delay.get()) {
            this.tick = 0;
            this.session.step(mc.field_1761, mc.field_1724);
            this.status = "сортирую " + this.session.progress() + "/" + this.session.total();
         }
      }
   }

   /** Starts sorting the currently open container. @return true if a sort started. */
   public boolean sortNow() {
      if (!this.isContainerOpen()) {
         return false;
      }
      int[] slots = this.containerSlots();
      if (slots.length > 1) {
         this.session = new SortSession(mc.field_1724.field_7512, slots);
         this.tick = 0;
         return true;
      }
      return false;
   }

   private boolean isContainerOpen() {
      return mc.field_1755 instanceof class_465 && this.containerSlots().length > 0;
   }

   private int[] containerSlots() {
      class_1703 handler = mc.field_1724.field_7512;
      class_1661 inv = mc.field_1724.method_31548();
      List<Integer> ids = new ArrayList<>();
      for (int i = 0; i < handler.field_7761.size(); ++i) {
         class_1735 slot = handler.field_7761.get(i);
         if (slot.field_7871 != inv) {
            ids.add(i);
         }
      }
      int[] out = new int[ids.size()];
      for (int i = 0; i < out.length; ++i) {
         out[i] = ids.get(i);
      }
      return out;
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList<>();
      lines.add("§7Статус: §r" + this.status);
      lines.add("§7Авто: " + (this.autoOnOpen.get() ? "§aВКЛ" : "§7ВЫКЛ"));
      return RenderUtil.panel(ctx, x, y, "Storage Sort", lines);
   }
}
