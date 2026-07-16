package dev.kafka.kafkautils.gui;

import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.module.modules.combat.EnchantHelper;
import dev.kafka.kafkautils.module.modules.render.PinnedOrder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;

/**
 * Preset picker for the enchant helper: choose a top-gear preset (Топ шлем,
 * Топ меч, …) for the item in your hand, see the missing books and the cheapest
 * order in a side panel, then either print it to chat or pin it to the HUD.
 * A preset only applies to a matching item (boots can't use the helmet preset) —
 * that check lives in {@link EnchantHelper}.
 */
public class EnchantHelperScreen extends class_437 {
   private static final int BG = 0xE6101014;
   private static final int HEADER = 0xFF1B1030;
   private static final int ACCENT = 0xFFB388FF;
   private static final int PANEL = 0x33000000;
   private static final int PANEL_X = 184;

   private final EnchantHelper helper;
   private String selectedKey;
   private List<String> currentLines = new ArrayList<>();

   public EnchantHelperScreen() {
      super(class_2561.method_43470("Зачарование"));
      this.helper = ModuleManager.get(EnchantHelper.class);
   }

   protected void method_25426() {
      if (this.helper == null) {
         return;
      }
      int y = 42;
      for (String key : this.helper.presetKeys()) {
         final String k = key;
         String label = (k.equals(this.selectedKey) ? "§d§l" : "§7") + this.helper.presetTooltip(k);
         this.method_37063(class_4185.method_46430(class_2561.method_43470(label), (b) -> {
            this.selectedKey = k;
            this.currentLines = this.helper.presetLines(k);
            this.method_41843();
         }).method_46434(16, y, 156, 14).method_46431());
         y += 16;
      }

      int by = this.field_22790 - 26;
      this.method_37063(class_4185.method_46430(class_2561.method_43470("§aВ чат"), (b) -> {
         if (this.selectedKey != null) {
            this.helper.printPreset(this.selectedKey);
         }
      }).method_46434(PANEL_X, by, 90, 18).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470("§dЗакрепить"), (b) -> {
         PinnedOrder po = ModuleManager.get(PinnedOrder.class);
         String key = this.selectedKey;
         EnchantHelper h = this.helper;
         if (po != null && key != null && h != null) {
            po.pin("Зачарование", () -> h.presetLines(key)); // live: updates as you grab the item/books
         }
      }).method_46434(PANEL_X + 96, by, 96, 18).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470("§7Открепить"), (b) -> {
         PinnedOrder po = ModuleManager.get(PinnedOrder.class);
         if (po != null) {
            po.unpin();
         }
      }).method_46434(PANEL_X + 196, by, 96, 18).method_46431());
   }

   public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
      int w = this.field_22789;
      int h = this.field_22790;
      ctx.method_25294(0, 0, w, h, BG);
      ctx.method_25294(0, 0, w, 30, HEADER);
      ctx.method_25294(0, 29, w, 31, ACCENT);
      ctx.method_51433(this.field_22793, "§5§lЗачарование §d§l— заготовки", 16, 11, 0xFFD9C2FF, true);
      ctx.method_51433(this.field_22793, "§7ESC", w - 32, 11, 0xFF9A8FB0, true);

      ctx.method_25294(PANEL_X - 6, 38, w - 12, h - 32, PANEL);
      super.method_25394(ctx, mouseX, mouseY, delta);

      if (this.currentLines.isEmpty()) {
         ctx.method_51433(this.field_22793, "§7Возьми предмет в руку и выбери заготовку слева.", PANEL_X + 4, 44, 0xFF9A8FB0, true);
      } else {
         int ty = 44;
         for (String line : this.currentLines) {
            ctx.method_51433(this.field_22793, line, PANEL_X + 4, ty, 0xFFE7DAF6, true);
            ty += 11;
         }
      }
   }

   public boolean method_25421() {
      return false;
   }
}
