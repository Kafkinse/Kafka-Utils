package dev.kafka.kafkautils.gui;

import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.module.modules.combat.BrewHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1842;
import net.minecraft.class_1844;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_6880;
import net.minecraft.class_7923;

/**
 * A searchable potion browser, like the creative menu but themed to match the
 * ClickGUI. Tabs switch between drink / splash (взрывное) / lingering (оседающее)
 * variants, hovering a bottle shows the game's own tooltip (name + duration),
 * and clicking one shows its cheapest brewing order (from {@link BrewHelper}).
 */
public class PotionBrowserScreen extends class_437 {
   private static final int BG = 0xE6101014;
   private static final int HEADER = 0xFF1B1030;
   private static final int ACCENT = 0xFFB388FF;
   private static final int PANEL = 0x33000000;

   private static final int GRID_X = 16;
   private static final int GRID_TOP = 84;
   private static final int CELL = 20;

   private final BrewHelper brew;
   private final List<String> keys;
   private final Map<String, class_1799[]> stacks = new HashMap<>();

   private int tab = BrewHelper.DRINK;
   private boolean ext;
   private boolean upg;
   private String selected;
   private String searchText = "";
   private class_342 search;

   public PotionBrowserScreen() {
      super(class_2561.method_43470("Зелья"));
      this.brew = ModuleManager.get(BrewHelper.class);
      this.keys = this.brew != null ? this.brew.brewKeys() : new ArrayList<>();
   }

   private int panelX() {
      return (int)(this.field_22789 * 0.60);
   }

   private int cols() {
      return Math.max(1, (this.panelX() - GRID_X - 8) / CELL);
   }

   protected void method_25426() {
      this.buildStacks();

      // Type tabs.
      String[] tabs = {"Питьевое", "Взрывное", "Оседающее"};
      for (int i = 0; i < tabs.length; ++i) {
         final int t = i;
         String label = (i == this.tab ? "§d§l" : "§7") + tabs[i];
         this.method_37063(class_4185.method_46430(class_2561.method_43470(label), (b) -> {
            this.tab = t;
            this.method_41843();
         }).method_46434(16 + i * 92, 34, 88, 18).method_46431());
      }

      // Modifier toggles.
      this.method_37063(class_4185.method_46430(class_2561.method_43470((this.ext ? "§a" : "§7") + "Удлинить"), (b) -> {
         this.ext = !this.ext;
         if (this.ext) {
            this.upg = false;
         }
         this.method_41843();
      }).method_46434(this.panelX() + 8, 34, 90, 18).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470((this.upg ? "§a" : "§7") + "Усилить II"), (b) -> {
         this.upg = !this.upg;
         if (this.upg) {
            this.ext = false;
         }
         this.method_41843();
      }).method_46434(this.panelX() + 102, 34, 90, 18).method_46431());

      // Search box.
      this.search = new class_342(this.field_22793, GRID_X, 58, this.panelX() - GRID_X - 8, 16, class_2561.method_43470("Поиск"));
      this.search.method_1880(48);
      this.search.method_1852(this.searchText);
      this.search.method_47404(class_2561.method_43470("§7Поиск зелья…"));
      this.search.method_1863((v) -> this.searchText = v);
      this.method_37063(this.search);
   }

   private void buildStacks() {
      if (!this.stacks.isEmpty()) {
         return;
      }
      class_1792 drink = itemOf("potion");
      class_1792 splash = itemOf("splash_potion");
      class_1792 linger = itemOf("lingering_potion");
      if (drink == null || splash == null || linger == null) {
         return;
      }
      class_1792[] items = {drink, splash, linger};
      for (String key : this.keys) {
         class_6880<class_1842> entry = potionEntry(key);
         class_1799[] arr = new class_1799[3];
         for (int t = 0; t < 3; ++t) {
            arr[t] = entry != null ? class_1844.method_57400(items[t], entry) : new class_1799(items[t]);
         }
         this.stacks.put(key, arr);
      }
   }

   /** Looks up an item by id via the item registry (no reliance on field ids). */
   private static class_1792 itemOf(String id) {
      Object ref = class_7923.field_41178.method_10223(class_2960.method_60656(id)).orElse(null);
      return ref == null ? null : (class_1792)((class_6880<?>)ref).comp_349();
   }

   @SuppressWarnings("unchecked")
   private static class_6880<class_1842> potionEntry(String key) {
      Object ref = class_7923.field_41179.method_10223(class_2960.method_60656(key)).orElse(null);
      return (class_6880<class_1842>)ref;
   }

   private List<String> filtered() {
      String q = this.searchText.trim().toLowerCase(Locale.ROOT);
      List<String> out = new ArrayList<>();
      for (String key : this.keys) {
         String ru = this.brew == null ? key : this.brew.brewTooltip(key);
         if (q.isEmpty() || key.contains(q) || ru.toLowerCase(Locale.ROOT).contains(q)) {
            out.add(key);
         }
      }
      return out;
   }

   public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
      int w = this.field_22789;
      int h = this.field_22790;
      ctx.method_25294(0, 0, w, h, BG);
      ctx.method_25294(0, 0, w, 30, HEADER);
      ctx.method_25294(0, 29, w, 31, ACCENT);
      ctx.method_51433(this.field_22793, "§5§lЗелья §d§l— варка", 16, 11, 0xFFD9C2FF, true);
      ctx.method_51433(this.field_22793, "§7ESC", w - 32, 11, 0xFF9A8FB0, true);

      int px = this.panelX();
      ctx.method_25294(GRID_X - 4, GRID_TOP - 6, px - 4, h - 12, PANEL);
      ctx.method_25294(px + 4, 58, w - 12, h - 12, PANEL);

      super.method_25394(ctx, mouseX, mouseY, delta);

      // Potion grid.
      List<String> list = this.filtered();
      int cols = this.cols();
      String hovered = null;
      for (int i = 0; i < list.size(); ++i) {
         String key = list.get(i);
         int x = GRID_X + (i % cols) * CELL;
         int y = GRID_TOP + (i / cols) * CELL;
         class_1799 stack = this.stacks.get(key)[this.tab];
         if (key.equals(this.selected)) {
            ctx.method_25294(x - 1, y - 1, x + 18, y + 18, 0x55B388FF);
         }
         if (stack != null) {
            ctx.method_51427(stack, x + 1, y + 1);
         }
         if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
            hovered = key;
         }
      }

      // Recipe panel for the selected potion.
      if (this.selected != null && this.brew != null) {
         int ty = 62;
         for (String line : this.brew.recipeLines(this.selected, this.tab, this.ext, this.upg)) {
            ctx.method_51433(this.field_22793, line, px + 8, ty, 0xFFE7DAF6, true);
            ty += 11;
         }
      } else {
         ctx.method_51433(this.field_22793, "§7Выбери зелье слева.", px + 8, 64, 0xFF9A8FB0, true);
      }

      // Hovered tooltip (drawn last, on top) — the game's own potion tooltip.
      if (hovered != null) {
         class_1799 stack = this.stacks.get(hovered)[this.tab];
         if (stack != null) {
            ctx.method_51446(this.field_22793, stack, mouseX, mouseY);
         }
      }
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      List<String> list = this.filtered();
      int cols = this.cols();
      for (int i = 0; i < list.size(); ++i) {
         int x = GRID_X + (i % cols) * CELL;
         int y = GRID_TOP + (i / cols) * CELL;
         if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
            this.selected = list.get(i);
            return true;
         }
      }
      return super.method_25402(mouseX, mouseY, button);
   }

   public boolean method_25421() {
      return false;
   }
}
