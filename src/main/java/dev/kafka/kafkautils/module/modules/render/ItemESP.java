package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.WorldRenderModule;
import dev.kafka.kafkautils.util.Render3D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.class_1297;
import net.minecraft.class_1542;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1814;
import net.minecraft.class_238;

public class ItemESP extends Module implements WorldRenderModule {
   private static final Set<class_1792> VALUABLE;
   private final List<class_238> boxes = new ArrayList();
   private int scanTimer = 0;

   public ItemESP() {
      super("Item ESP", "Highlights valuable dropped items.", Category.RENDER);
   }

   protected void onEnable() {
      this.scanTimer = 0;
      this.boxes.clear();
   }

   public void onTick() {
      if (mc.field_1687 != null) {
         if (this.scanTimer-- <= 0) {
            this.scanTimer = 5;
            this.boxes.clear();

            for(class_1297 e : mc.field_1687.method_18112()) {
               if (e instanceof class_1542) {
                  class_1542 ie = (class_1542)e;
                  if (isValuable(ie.method_6983())) {
                     this.boxes.add(ie.method_5829());
                  }
               }
            }

         }
      }
   }

   public void onWorldRender(WorldRenderContext ctx) {
      for(class_238 b : this.boxes) {
         Render3D.drawBox((class_238)b, -29696);
      }

   }

   private static boolean isValuable(class_1799 s) {
      return VALUABLE.contains(s.method_7909()) || s.method_7932() != class_1814.field_8906;
   }

   static {
      VALUABLE = Set.of(class_1802.field_8477, class_1802.field_8603, class_1802.field_22020, class_1802.field_22021, class_1802.field_22018, class_1802.field_22019, class_1802.field_8687, class_1802.field_8288, class_1802.field_8463, class_1802.field_8367, class_1802.field_8833, class_1802.field_8137, class_1802.field_8598, class_1802.field_49814, class_1802.field_49813, class_1802.field_49098);
   }
}
