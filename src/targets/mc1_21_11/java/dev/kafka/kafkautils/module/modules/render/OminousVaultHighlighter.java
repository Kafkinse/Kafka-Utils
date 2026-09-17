package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.WorldRenderModule;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.util.Render3D;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.class_1923;
import net.minecraft.class_2338;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_2791;
import net.minecraft.class_2818;
import net.minecraft.class_332;
import net.minecraft.class_9199;

public class OminousVaultHighlighter extends Module implements WorldRenderModule, HudModule {
   private static final int SCAN_INTERVAL = 20;
   private static final int MAX_RADIUS = 8;
   private final BooleanSetting showOminous = (BooleanSetting)this.add(new BooleanSetting("Ominous", true));
   private final BooleanSetting showNormal = (BooleanSetting)this.add(new BooleanSetting("Normal", false));
   private final List<class_2338> ominous = new ArrayList();
   private final List<class_2338> normal = new ArrayList();
   private int scanTimer = 0;

   public OminousVaultHighlighter() {
      super("Ominous Vault ESP", "Highlights un-opened vaults through walls.", Category.RENDER);
   }

   protected void onEnable() {
      this.scanTimer = 0;
      this.ominous.clear();
      this.normal.clear();
   }

   protected void onDisable() {
      this.ominous.clear();
      this.normal.clear();
   }

   public void onTick() {
      if (mc.field_1687 != null && mc.field_1724 != null) {
         if (this.scanTimer-- <= 0) {
            this.scanTimer = 20;
            this.rescan();
         }
      }
   }

   private void rescan() {
      this.ominous.clear();
      this.normal.clear();
      int radius = Math.min(8, (Integer)mc.field_1690.method_42503().method_41753());
      class_1923 center = mc.field_1724.method_31476();

      for(int cx = center.field_9181 - radius; cx <= center.field_9181 + radius; ++cx) {
         for(int cz = center.field_9180 - radius; cz <= center.field_9180 + radius; ++cz) {
            class_2791 chunk = mc.field_1687.method_8497(cx, cz);
            if (chunk instanceof class_2818 worldChunk) {
               for(class_2586 be : worldChunk.method_12214().values()) {
                  if (be instanceof class_9199) {
                     class_2338 pos = be.method_11016();
                     class_2680 state = mc.field_1687.method_8320(pos);
                     String s = state.toString().toLowerCase();
                     if (!s.contains("vault_state=ejecting") && !s.contains("vault_state=unlocking")) {
                        if (s.contains("ominous=true")) {
                           if (this.showOminous.get()) {
                              this.ominous.add(pos.method_10062());
                           }
                        } else if (this.showNormal.get()) {
                           this.normal.add(pos.method_10062());
                        }
                     }
                  }
               }
            }
         }
      }

   }

   public void onWorldRender(WorldRenderContext ctx) {
      for(class_2338 pos : this.ominous) {
         Render3D.drawBox(pos, -13388289);
      }

      for(class_2338 pos : this.normal) {
         Render3D.drawBox((class_2338)pos, -29696);
      }

   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList();
      lines.add("§dЗловещих: §r" + this.ominous.size());
      lines.add("§7Обычных: §r" + this.normal.size());
      return RenderUtil.panel(ctx, x, y, "Ominous Vaults", lines);
   }
}
