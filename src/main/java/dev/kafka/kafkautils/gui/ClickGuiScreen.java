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
import dev.kafka.kafkautils.util.StaffCounter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;

public class ClickGuiScreen extends class_437 {
   private static final int COL_WIDTH = 152;
   private static final int COL_GAP = 12;
   private static final int START_X = 20;
   private static final int HEADER_Y = 38;
   private static final Map<String, Boolean> expanded = new HashMap();

   public ClickGuiScreen() {
      super(class_2561.method_43470("Kafka Utils"));
   }

   protected void method_25426() {
      int x = 20;

      for(Category category : Category.values()) {
         int y = 56;

         for(Module module : ModuleManager.getByCategory(category)) {
            boolean hasSettings = !module.getSettings().isEmpty();
            int toggleW = hasSettings ? 136 : 152;
            class_4185 toggle = class_4185.method_46430(this.moduleLabel(module), (b) -> {
               module.toggle();
               b.method_25355(this.moduleLabel(module));
               ConfigManager.save();
            }).method_46434(x, y, toggleW, 16).method_46431();
            this.method_37063(toggle);
            if (hasSettings) {
               boolean exp = (Boolean)expanded.getOrDefault(module.getName(), false);
               this.method_37063(class_4185.method_46430(class_2561.method_43470(exp ? "−" : "+"), (b) -> {
                  expanded.put(module.getName(), !exp);
                  this.method_41843();
               }).method_46434(x + 152 - 14, y, 14, 16).method_46431());
            }

            y += 18;
            if (hasSettings && (Boolean)expanded.getOrDefault(module.getName(), false)) {
               for(Setting s : module.getSettings()) {
                  y = this.addSetting(x + 8, y, 144, s);
               }
            }

            y += 5;
         }

         x += 164;
      }

      class_4185 hudBtn = class_4185.method_46430(class_2561.method_43470("§d§lHUD Editor"), (b) -> {
         if (this.field_22787 != null) {
            this.field_22787.method_1507(new HudEditorScreen());
         }

      }).method_46434(20, this.field_22790 - 26, 120, 18).method_46431();
      this.method_37063(hudBtn);
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
            field.method_47404(class_2561.method_43470("§7" + s.getName() + " (через запятую)"));
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

         for(int i = 0; i < vals.size(); ++i) {
            int idx = i;
            class_342 field = new class_342(this.field_22793, x, y, w - 16, 14, class_2561.method_43470(s.getName()));
            field.method_1880(128);
            field.method_1852((String)vals.get(i));
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
      String state = m.isEnabled() ? "§a● " : "§7○ ";
      return class_2561.method_43470(state + "§r" + m.getName());
   }

   private class_2561 boolLabel(BooleanSetting s) {
      String v = s.get() ? "§aON" : "§7OFF";
      String var10000 = s.getName();
      return class_2561.method_43470("§r" + var10000 + ": " + v);
   }

   private class_2561 numLabel(NumberSetting s) {
      String var10000 = s.getName();
      return class_2561.method_43470("§r" + var10000 + ": §d" + s.get());
   }

   private class_2561 modeLabel(ModeSetting s) {
      String var10000 = s.getName();
      return class_2561.method_43470("§r" + var10000 + ": §d" + s.get());
   }

   public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
      ctx.method_25294(0, 0, this.field_22789, this.field_22790, -803995624);
      ctx.method_51433(this.field_22793, "§5§lKafka Utils §r§7— ESC to close", 20, 14, -1517825, true);
      int[] sc = StaffCounter.counts();
      ctx.method_51433(this.field_22793, "§7Стафф онлайн:  §dⒽ §r" + sc[0] + "§d   Ⓜ §r" + sc[1] + "§d   Ⓐ §r" + sc[2], 240, 14, -1517825, true);
      int sy = 26;

      for(String n : StaffCounter.staffNames()) {
         ctx.method_51433(this.field_22793, "§d" + n, 240, sy, -1517825, true);
         sy += 11;
         if (sy > this.field_22790 - 40) {
            break;
         }
      }

      int x = 20;

      for(Category category : Category.values()) {
         ctx.method_25294(x, 38, x + 152, 52, -12976032);
         ctx.method_51433(this.field_22793, "§d§l" + category.getTitle(), x + 4, 41, -1517825, false);
         x += 164;
      }

      super.method_25394(ctx, mouseX, mouseY, delta);
   }

   public boolean method_25421() {
      return false;
   }
}
