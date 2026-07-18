package dev.kafka.kafkautils.gui;

import dev.kafka.kafkautils.config.ConfigManager;
import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.ListSetting;
import dev.kafka.kafkautils.setting.ModeSetting;
import dev.kafka.kafkautils.setting.NumberSetting;
import dev.kafka.kafkautils.setting.Setting;
import dev.kafka.kafkautils.setting.StringSetting;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;

/**
 * Tabbed, themed ClickGUI: a header bar, a row of category tabs, and the selected
 * category's modules laid out in two balanced columns with expandable settings.
 */
public class ClickGuiScreen extends class_437 {
   private static final int COL_A = 20;
   private static final int COL_B = 232;
   private static final int COL_W = 200;
   private static final int TAB_X = 20;
   private static final int TAB_Y = 34;
   private static final int TAB_W = 90;
   private static final int TAB_H = 18;
   private static final int TAB_GAP = 4;

   private static final int BG = 0xE6101014;
   private static final int HEADER = 0xFF1B1030;
   private static final int ACCENT = 0xFFB388FF;
   private static final int PANEL = 0x33000000;

   private static final Map<String, Boolean> expanded = new HashMap<>();
   private static int selectedCategory = 0;

   public ClickGuiScreen() {
      super(class_2561.method_43470("Kafka Utils"));
   }

   protected void method_25426() {
      Category[] cats = Category.values();
      if (selectedCategory >= cats.length) {
         selectedCategory = 0;
      }

      for (int i = 0; i < cats.length; ++i) {
         final int idx = i;
         String label = (i == selectedCategory ? "§d§l" : "§7") + cats[i].getTitle();
         this.method_37063(class_4185.method_46430(class_2561.method_43470(label), (b) -> {
            selectedCategory = idx;
            this.method_41843();
         }).method_46434(TAB_X + i * (TAB_W + TAB_GAP), TAB_Y, TAB_W, TAB_H).method_46431());
      }

      int top = TAB_Y + TAB_H + 12;
      int[] colX = {COL_A, COL_B};
      int[] colY = {top, top};

      for (Module module : ModuleManager.getByCategory(cats[selectedCategory])) {
         int c = colY[0] <= colY[1] ? 0 : 1;
         int x = colX[c];
         int y = colY[c];

         boolean hasSettings = !module.getSettings().isEmpty();
         int toggleW = hasSettings ? COL_W - 14 : COL_W;
         this.method_37063(class_4185.method_46430(this.moduleLabel(module), (b) -> {
            module.toggle();
            b.method_25355(this.moduleLabel(module));
            ConfigManager.save();
         }).method_46434(x, y, toggleW, 16).method_46431());

         if (hasSettings) {
            boolean exp = expanded.getOrDefault(module.getName(), false);
            this.method_37063(class_4185.method_46430(class_2561.method_43470(exp ? "§d−" : "§a+"), (b) -> {
               expanded.put(module.getName(), !exp);
               this.method_41843();
            }).method_46434(x + COL_W - 12, y, 12, 16).method_46431());
         }

         y += 18;
         if (hasSettings && expanded.getOrDefault(module.getName(), false)) {
            for (Setting s : module.getSettings()) {
               y = this.addSetting(x + 8, y, COL_W - 16, s);
            }
         }
         y += 6;
         colY[c] = y;
      }

      this.method_37063(class_4185.method_46430(class_2561.method_43470("§d§lHUD Editor"), (b) -> {
         if (this.field_22787 != null) {
            this.field_22787.method_1507(new HudEditorScreen());
         }
      }).method_46434(20, this.field_22790 - 24, 120, 18).method_46431());
   }

   private int addSetting(int x, int y, int w, Setting s) {
      if (s instanceof BooleanSetting bs) {
         this.method_37063(class_4185.method_46430(this.boolLabel(bs), (b) -> {
            bs.toggle();
            b.method_25355(this.boolLabel(bs));
            ConfigManager.save();
         }).method_46434(x, y, w, 14).method_46431());
         return y + 16;
      } else if (s instanceof NumberSetting ns) {
         this.method_37063(class_4185.method_46430(this.numLabel(ns), (b) -> {
            ns.cycle();
            b.method_25355(this.numLabel(ns));
            ConfigManager.save();
         }).method_46434(x, y, w, 14).method_46431());
         return y + 16;
      } else if (s instanceof ModeSetting ms) {
         this.method_37063(class_4185.method_46430(this.modeLabel(ms), (b) -> {
            ms.cycle();
            b.method_25355(this.modeLabel(ms));
            ConfigManager.save();
         }).method_46434(x, y, w, 14).method_46431());
         return y + 16;
      } else if (!(s instanceof ListSetting list)) {
         if (s instanceof StringSetting ss) {
            class_342 field = new class_342(this.field_22793, x, y, w, 14, class_2561.method_43470(s.getName()));
            field.method_1880(256);
            field.method_1852(ss.get());
            field.method_47404(class_2561.method_43470("§7" + s.getName()));
            field.method_1863((v) -> {
               ss.set(v);
               ConfigManager.save();
            });
            this.method_37063(field);
            return y + 18;
         } else {
            return y;
         }
      } else {
         List<String> vals = list.values();
         for (int i = 0; i < vals.size(); ++i) {
            int idx = i;
            class_342 field = new class_342(this.field_22793, x, y, w - 16, 14, class_2561.method_43470(s.getName()));
            field.method_1880(128);
            field.method_1852(vals.get(i));
            field.method_1863((v) -> {
               list.setEntry(idx, v);
               ConfigManager.save();
            });
            this.method_37063(field);
            this.method_37063(class_4185.method_46430(class_2561.method_43470("§cx"), (b) -> {
               list.removeEntry(idx);
               ConfigManager.save();
               this.method_41843();
            }).method_46434(x + w - 14, y, 14, 14).method_46431());
            y += 16;
         }
         this.method_37063(class_4185.method_46430(class_2561.method_43470("§a+ " + s.getName()), (b) -> {
            list.addEntry("");
            ConfigManager.save();
            this.method_41843();
         }).method_46434(x, y, w, 14).method_46431());
         return y + 16;
      }
   }

   private class_2561 moduleLabel(Module m) {
      String state = m.isEnabled() ? "§a● " : "§8○ ";
      return class_2561.method_43470(state + "§r" + m.getName());
   }

   private class_2561 boolLabel(BooleanSetting s) {
      return class_2561.method_43470("§r" + s.getName() + ": " + (s.get() ? "§aВКЛ" : "§8ВЫКЛ"));
   }

   private class_2561 numLabel(NumberSetting s) {
      return class_2561.method_43470("§r" + s.getName() + ": §d" + s.get());
   }

   private class_2561 modeLabel(ModeSetting s) {
      return class_2561.method_43470("§r" + s.getName() + ": §d" + s.get());
   }

   public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
      int w = this.field_22789;
      int h = this.field_22790;
      ctx.method_25294(0, 0, w, h, BG);
      ctx.method_25294(0, 0, w, 30, HEADER);
      ctx.method_25294(0, 29, w, 30, ACCENT);
      ctx.method_51433(this.field_22793, "§5§lKafka §d§lUtils", 20, 11, 0xFFD9C2FF, true);
      ctx.method_51433(this.field_22793, "§7ESC", w - 32, 11, 0xFF9A8FB0, true);

      Category[] cats = Category.values();
      for (int i = 0; i < cats.length; ++i) {
         if (i == selectedCategory) {
            int tx = TAB_X + i * (TAB_W + TAB_GAP);
            ctx.method_25294(tx, TAB_Y + TAB_H, tx + TAB_W, TAB_Y + TAB_H + 2, ACCENT);
         }
      }

      ctx.method_25294(14, TAB_Y + TAB_H + 8, w - 14, h - 30, PANEL);
      super.method_25394(ctx, mouseX, mouseY, delta);
   }

   public boolean method_25421() {
      return false;
   }
}
