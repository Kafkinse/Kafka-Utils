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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

/**
 * Modern dark ClickGUI: a category sidebar, a greeting header with search, and
 * scrollable two-column module cards with custom switches, sliders and dropdown
 * values. Text values are edited in a small centred modal (the only text
 * widgets); everything else is drawn and hit-tested directly.
 */
public class ClickGuiScreen extends class_437 {
   // Palette (ARGB).
   private static final int SCRIM = 0xCC05050A;
   private static final int WIN = 0xFF0E0E13;
   private static final int SB = 0xFF131319;
   private static final int CARD = 0xFF191922;
   private static final int CARD_HDR = 0xFF20202B;
   private static final int BORDER = 0xFF2A2A37;
   private static final int ACCENT = 0xFF8B5CF6;
   private static final int ACCENT2 = 0xFFA78BFA;
   private static final int TEXT = 0xFFE7E5EE;
   private static final int MUTED = 0xFF8B8898;
   private static final int FAINT = 0xFF57556A;
   private static final int OFF = 0xFF34323F;
   private static final int TRACK = 0xFF2A2833;
   private static final int GREEN = 0xFF46D17A;

   private static int selectedCategory = 0;
   private static int scroll = 0;

   private final List<Object[]> hits = new ArrayList<>(); // {x,y,w,h,type,ref,ref2}
   private int winX, winY, winW, winH, sbW, contentX, contentTop, contentBottom, colW, colGap;
   private int maxScroll;

   private class_342 search;

   // Text editor modal state.
   private StringSetting editStr;
   private ListSetting editList;
   private int editListIdx = -1;
   private class_342 editor;

   public ClickGuiScreen() {
      super(class_2561.method_43470("Kafka Utils"));
   }

   protected void method_25426() {
      Category[] cats = Category.values();
      if (selectedCategory >= cats.length) {
         selectedCategory = 0;
      }
      this.geom();

      boolean editing = this.editStr != null || this.editList != null;

      // Search field (top-right of the header) — hidden while the modal is open.
      if (!editing) {
         String prev = this.search != null ? this.search.method_1882() : "";
         int shW = 150;
         this.search = new class_342(this.field_22793, contentX + colW * 2 + colGap - shW, winY + 20, shW, 16, class_2561.method_43470("поиск"));
         this.search.method_1858(false);
         this.search.method_1868(TEXT);
         this.search.method_47404(class_2561.method_43470("§7поиск модуля…"));
         this.search.method_1852(prev);
         this.method_37063(this.search);
      }

      // Text editor modal (only present while editing a value).
      if (editing) {
         int ew = 260;
         this.editor = new class_342(this.field_22793, this.field_22789 / 2 - ew / 2, this.field_22790 / 2 - 6, ew, 16, class_2561.method_43470("значение"));
         this.editor.method_1880(256);
         this.editor.method_1868(TEXT);
         this.editor.method_1852(this.currentEditValue());
         this.method_37063(this.editor);
         this.method_48265(this.editor);
      }
   }

   private void geom() {
      int w = this.field_22789;
      int h = this.field_22790;
      winX = 22;
      winY = 18;
      winW = w - 44;
      winH = h - 36;
      sbW = 152;
      contentX = winX + sbW + 12;
      contentTop = winY + 58;
      contentBottom = winY + winH - 12;
      int avail = winX + winW - 12 - contentX;
      colGap = 12;
      colW = (avail - colGap) / 2;
   }

   // --- layout + draw + hit recording -------------------------------------

   public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
      this.geom();
      this.hits.clear();
      int w = this.field_22789;
      int h = this.field_22790;

      ctx.method_25294(0, 0, w, h, SCRIM);
      fillRound(ctx, winX, winY, winW, winH, 8, WIN);
      fillRound(ctx, winX, winY, sbW, winH, 8, SB);

      this.drawSidebar(ctx, mouseX, mouseY);
      this.drawHeader(ctx);

      // Scrollable module area.
      ctx.method_44379(contentX - 4, contentTop - 2, winX + winW - 6, contentBottom);
      this.drawCards(ctx, mouseX, mouseY);
      ctx.method_44380();

      // Modal panel is drawn before super so the editor text field lands on top.
      if (this.editStr != null || this.editList != null) {
         this.drawEditor(ctx);
      }

      super.method_25394(ctx, mouseX, mouseY, delta); // search field or editor field
   }

   private void drawSidebar(class_332 ctx, int mouseX, int mouseY) {
      ctx.method_51433(this.field_22793, "§f§lKafka §r§7Utils", winX + 16, winY + 18, TEXT, false);
      ctx.method_25294(winX + 14, winY + 34, winX + sbW - 14, winY + 35, BORDER);

      Category[] cats = Category.values();
      int y = winY + 46;
      for (int i = 0; i < cats.length; ++i) {
         boolean sel = i == selectedCategory;
         int itemH = 30;
         if (sel) {
            fillRound(ctx, winX + 10, y, sbW - 20, itemH, 5, CARD_HDR);
            ctx.method_25294(winX + 10, y + 6, winX + 12, y + itemH - 6, ACCENT);
         }
         // icon square
         fillRound(ctx, winX + 18, y + 7, 16, 16, 4, sel ? ACCENT : OFF);
         String initial = cats[i].getTitle().substring(0, 1).toUpperCase(Locale.ROOT);
         ctx.method_51433(this.field_22793, initial, winX + 23, y + 11, sel ? 0xFFFFFFFF : MUTED, false);
         ctx.method_51433(this.field_22793, (sel ? "§f" : "§r") + cats[i].getTitle(), winX + 42, y + 7, sel ? TEXT : MUTED, false);
         int count = ModuleManager.getByCategory(cats[i]).size();
         ctx.method_51433(this.field_22793, "§8" + count + " модулей", winX + 42, y + 17, FAINT, false);
         this.hits.add(new Object[]{winX + 10, y, sbW - 20, itemH, "cat", i, null});
         y += itemH + 4;
      }

      // Bottom player card.
      int by = winY + winH - 40;
      ctx.method_25294(winX + 14, by - 8, winX + sbW - 14, by - 7, BORDER);
      String name = this.playerName();
      fillRound(ctx, winX + 16, by, 20, 20, 5, ACCENT);
      ctx.method_51433(this.field_22793, name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT), winX + 23, by + 6, 0xFFFFFFFF, false);
      ctx.method_51433(this.field_22793, "§f" + name, winX + 42, by + 3, TEXT, false);
      ctx.method_25294(winX + 42, by + 15, winX + 46, by + 19, GREEN);
      ctx.method_51433(this.field_22793, "§7в сети", winX + 50, by + 12, MUTED, false);
   }

   private void drawHeader(class_332 ctx) {
      String name = this.playerName();
      ctx.method_51433(this.field_22793, "§f§lПривет, " + name, contentX, winY + 16, TEXT, false);
      ctx.method_51433(this.field_22793, "§7С возвращением", contentX, winY + 28, MUTED, false);
      int shW = 150;
      int sx = contentX + colW * 2 + colGap - shW;
      fillRound(ctx, sx - 6, winY + 16, shW + 6, 22, 5, CARD);
      int hb = sx - 6 - 92;
      fillRound(ctx, hb, winY + 16, 86, 22, 5, CARD);
      ctx.method_51433(this.field_22793, "§7HUD Editor", hb + 14, winY + 22, MUTED, false);
      this.hits.add(new Object[]{hb, winY + 16, 86, 22, "hud", null, null});
   }

   private void drawCards(class_332 ctx, int mouseX, int mouseY) {
      String q = this.search != null ? this.search.method_1882().trim().toLowerCase(Locale.ROOT) : "";
      List<Module> mods = new ArrayList<>();
      for (Module m : ModuleManager.getByCategory(Category.values()[selectedCategory])) {
         if (q.isEmpty() || m.getName().toLowerCase(Locale.ROOT).contains(q)) {
            mods.add(m);
         }
      }

      int[] colX = {contentX, contentX + colW + colGap};
      int[] colY = {contentTop - scroll, contentTop - scroll};
      for (Module m : mods) {
         int ch = this.cardHeight(m);
         int c = colY[0] <= colY[1] ? 0 : 1;
         int x = colX[c];
         int y = colY[c];
         this.drawCard(ctx, m, x, y, colW, ch);
         colY[c] = y + ch + colGap;
      }
      int bottom = Math.max(colY[0], colY[1]) + scroll;
      int totalH = bottom - contentTop;
      this.maxScroll = Math.max(0, totalH - (contentBottom - contentTop));
      if (scroll > this.maxScroll) {
         scroll = this.maxScroll;
      }
      if (scroll < 0) {
         scroll = 0;
      }
   }

   private int cardHeight(Module m) {
      return 24 + m.getSettings().size() * 20 + 8;
   }

   private void drawCard(class_332 ctx, Module m, int x, int y, int w, int h) {
      if (y + h < contentTop || y > contentBottom) {
         return; // fully outside the viewport
      }
      fillRound(ctx, x, y, w, h, 6, CARD);
      fillRound(ctx, x, y, w, 24, 6, CARD_HDR);
      ctx.method_25294(x + 10, y + 24, x + w - 10, y + 25, BORDER);
      ctx.method_51433(this.field_22793, "§f" + m.getName(), x + 12, y + 8, TEXT, false);
      this.drawSwitch(ctx, x + w - 30, y + 7, m.isEnabled());
      if (y + 4 >= contentTop && y <= contentBottom) {
         this.hits.add(new Object[]{x + w - 32, y + 4, 28, 18, "modtoggle", m, null});
      }

      int ry = y + 30;
      for (Setting s : m.getSettings()) {
         this.drawSetting(ctx, s, x + 12, ry, w - 24);
         ry += 20;
      }
   }

   private void drawSetting(class_332 ctx, Setting s, int x, int y, int w) {
      if (y + 14 < contentTop || y > contentBottom) {
         return; // outside the scrolled viewport — don't draw or hit-test
      }
      ctx.method_51433(this.field_22793, "§7" + s.getName(), x, y + 4, MUTED, false);
      int right = x + w;
      if (s instanceof BooleanSetting bs) {
         this.drawSwitch(ctx, right - 22, y + 2, bs.get());
         this.hits.add(new Object[]{right - 24, y, 26, 16, "bool", bs, null});
      } else if (s instanceof NumberSetting ns) {
         int val = ns.get();
         String vs = Integer.toString(val);
         int vw = this.field_22793.method_1727(vs);
         ctx.method_51433(this.field_22793, "§f" + vs, right - vw, y + 4, TEXT, false);
         int trackX = x + w / 2 - 6;
         int trackW = right - vw - 8 - trackX;
         if (trackW < 20) {
            trackX = x + 70;
            trackW = right - vw - 8 - trackX;
         }
         int ty = y + 6;
         fillRound(ctx, trackX, ty, trackW, 4, 2, TRACK);
         double frac = ns.max() > ns.min() ? (double) (val - ns.min()) / (ns.max() - ns.min()) : 0;
         int fillW = (int) (trackW * frac);
         if (fillW > 0) {
            fillRound(ctx, trackX, ty, fillW, 4, 2, ACCENT);
         }
         int knobX = trackX + fillW;
         fillRound(ctx, knobX - 3, ty - 3, 6, 10, 3, ACCENT2);
         this.hits.add(new Object[]{trackX - 3, y, trackW + 12, 16, "num", ns, new int[]{trackX, trackW}});
      } else if (s instanceof ModeSetting ms) {
         String vs = ms.get();
         int vw = this.field_22793.method_1727(vs) + 12;
         fillRound(ctx, right - vw, y + 1, vw, 14, 4, CARD_HDR);
         ctx.method_51433(this.field_22793, "§f" + vs + " §7▾", right - vw + 5, y + 4, TEXT, false);
         this.hits.add(new Object[]{right - vw, y, vw, 16, "mode", ms, null});
      } else if (s instanceof ListSetting ls) {
         ctx.method_51433(this.field_22793, "§8" + ls.values().size() + " зап. §7[ред.]", right - this.field_22793.method_1727(ls.values().size() + " зап. [ред.]") - 4, y + 4, MUTED, false);
         this.hits.add(new Object[]{x, y, w, 16, "listopen", ls, null});
      } else if (s instanceof StringSetting ss) {
         String v = ss.get();
         String show = v.length() > 22 ? v.substring(0, 21) + "…" : v;
         if (show.isEmpty()) {
            show = "§8—";
         }
         int vw = this.field_22793.method_1727(show.replace("§8", "")) + 12;
         if (vw > w - 60) {
            vw = w - 60;
         }
         fillRound(ctx, right - vw, y + 1, vw, 14, 4, CARD_HDR);
         ctx.method_51433(this.field_22793, "§f" + show, right - vw + 5, y + 4, TEXT, false);
         this.hits.add(new Object[]{right - vw, y, vw, 16, "str", ss, null});
      }
   }

   private void drawSwitch(class_332 ctx, int x, int y, boolean on) {
      fillRound(ctx, x, y, 20, 11, 5, on ? ACCENT : OFF);
      int knobX = on ? x + 10 : x + 1;
      fillRound(ctx, knobX, y + 1, 9, 9, 4, 0xFFEDEBF3);
   }

   private void drawEditor(class_332 ctx) {
      int cx = this.field_22789 / 2;
      int cy = this.field_22790 / 2;
      int ew = 260;
      ctx.method_25294(0, 0, this.field_22789, this.field_22790, 0x99000000);
      fillRound(ctx, cx - ew / 2 - 12, cy - 44, ew + 24, 92, 8, CARD);
      fillRound(ctx, cx - ew / 2 - 12, cy - 44, ew + 24, 22, 8, CARD_HDR);
      String title = this.editList != null
         ? (this.editListIdx < 0 ? "Новая запись" : "Изменить запись")
         : "Изменить: " + (this.editStr != null ? this.editStr.getName() : "");
      ctx.method_51433(this.field_22793, "§f" + title, cx - ew / 2 - 4, cy - 37, TEXT, false);
      fillRound(ctx, cx - ew / 2 - 6, cy - 10, ew + 12, 24, 5, WIN);

      int by = cy + 22;
      fillRound(ctx, cx - ew / 2 - 12 + 12, by, 90, 18, 5, ACCENT);
      ctx.method_51433(this.field_22793, "§fСохранить", cx - ew / 2 + 22, by + 5, 0xFFFFFFFF, false);
      this.hits.add(new Object[]{cx - ew / 2, by, 90, 18, "editorSave", null, null});
      fillRound(ctx, cx + ew / 2 - 90 + 12, by, 90, 18, 5, OFF);
      ctx.method_51433(this.field_22793, "§7Отмена", cx + ew / 2 - 48, by + 5, MUTED, false);
      this.hits.add(new Object[]{cx + ew / 2 - 78, by, 90, 18, "editorCancel", null, null});

      if (this.editList != null && this.editListIdx >= 0) {
         fillRound(ctx, cx + ew / 2 - 90 + 12, by - 24, 90, 16, 5, 0xFF3A2030);
         ctx.method_51433(this.field_22793, "§cУдалить запись", cx + ew / 2 - 68, by - 20, 0xFFFF8080, false);
         this.hits.add(new Object[]{cx + ew / 2 - 78, by - 24, 90, 16, "editorDelete", null, null});
      }
   }

   // --- input -------------------------------------------------------------

   public boolean method_25402(class_11909 click, boolean doubled) {
      if (click.method_74245() == 0) {
         int mx = (int) click.comp_4798();
         int my = (int) click.comp_4799();
         boolean modal = this.editStr != null || this.editList != null;
         for (Object[] hb : this.hits) {
            int x = (int) hb[0];
            int y = (int) hb[1];
            int w = (int) hb[2];
            int h = (int) hb[3];
            String type = (String) hb[4];
            boolean editorHit = type.startsWith("editor");
            if (modal != editorHit) {
               continue; // modal blocks content hits and vice-versa
            }
            if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
               if (this.handleHit(type, hb, mx)) {
                  return true;
               }
            }
         }
      }
      return super.method_25402(click, doubled);
   }

   private boolean handleHit(String type, Object[] hb, int mx) {
      switch (type) {
         case "cat" -> {
            selectedCategory = (int) hb[5];
            scroll = 0;
            return true;
         }
         case "modtoggle" -> {
            ((Module) hb[5]).toggle();
            ConfigManager.save();
            return true;
         }
         case "hud" -> {
            class_310.method_1551().method_1507(new HudEditorScreen());
            return true;
         }
         case "bool" -> {
            ((BooleanSetting) hb[5]).toggle();
            ConfigManager.save();
            return true;
         }
         case "mode" -> {
            ((ModeSetting) hb[5]).cycle();
            ConfigManager.save();
            return true;
         }
         case "num" -> {
            int[] tr = (int[]) hb[6];
            this.applySlider((NumberSetting) hb[5], tr[0], tr[1], mx);
            this.draggingNum = (NumberSetting) hb[5];
            this.dragTrackX = tr[0];
            this.dragTrackW = tr[1];
            return true;
         }
         case "str" -> {
            this.editStr = (StringSetting) hb[5];
            this.editList = null;
            this.method_41843();
            return true;
         }
         case "listopen" -> {
            this.editList = (ListSetting) hb[5];
            this.editListIdx = this.editList.values().isEmpty() ? -1 : 0;
            this.editStr = null;
            this.method_41843();
            return true;
         }
         case "editorSave" -> {
            this.commitEditor();
            return true;
         }
         case "editorCancel" -> {
            this.closeEditor();
            return true;
         }
         case "editorDelete" -> {
            if (this.editList != null && this.editListIdx >= 0) {
               this.editList.removeEntry(this.editListIdx);
               ConfigManager.save();
            }
            this.closeEditor();
            return true;
         }
         default -> {
            return false;
         }
      }
   }

   private NumberSetting draggingNum;
   private int dragTrackX;
   private int dragTrackW;

   public boolean method_25403(class_11909 click, double dx, double dy) {
      if (this.draggingNum != null) {
         this.applySlider(this.draggingNum, this.dragTrackX, this.dragTrackW, (int) click.comp_4798());
         return true;
      }
      return super.method_25403(click, dx, dy);
   }

   public boolean method_25406(class_11909 click) {
      if (this.draggingNum != null) {
         this.draggingNum = null;
         ConfigManager.save();
         return true;
      }
      return super.method_25406(click);
   }

   private void applySlider(NumberSetting ns, int trackX, int trackW, int mx) {
      if (trackW <= 0) {
         return;
      }
      double frac = Math.max(0.0, Math.min(1.0, (double) (mx - trackX) / trackW));
      int step = Math.max(1, ns.step());
      long raw = Math.round(ns.min() + frac * (ns.max() - ns.min()));
      long snapped = ns.min() + Math.round((double) (raw - ns.min()) / step) * step;
      ns.set((int) snapped);
   }

   public boolean method_25401(double mouseX, double mouseY, double horiz, double vert) {
      if (this.editStr == null && this.editList == null && vert != 0) {
         scroll -= (int) Math.round(vert) * 22;
         if (scroll < 0) {
            scroll = 0;
         }
         if (scroll > this.maxScroll) {
            scroll = this.maxScroll;
         }
         return true;
      }
      return super.method_25401(mouseX, mouseY, horiz, vert);
   }

   public boolean method_25404(class_11908 key) {
      int code = key.comp_4795();
      if (this.editStr != null || this.editList != null) {
         if (code == GLFW.GLFW_KEY_ENTER || code == GLFW.GLFW_KEY_KP_ENTER) {
            this.commitEditor();
            return true;
         }
         if (code == GLFW.GLFW_KEY_ESCAPE) {
            this.closeEditor();
            return true;
         }
         return super.method_25404(key);
      }
      return super.method_25404(key);
   }

   // --- editor helpers ----------------------------------------------------

   private String currentEditValue() {
      if (this.editStr != null) {
         return this.editStr.get();
      }
      if (this.editList != null && this.editListIdx >= 0 && this.editListIdx < this.editList.values().size()) {
         return this.editList.values().get(this.editListIdx);
      }
      return "";
   }

   private void commitEditor() {
      String v = this.editor != null ? this.editor.method_1882() : "";
      if (this.editStr != null) {
         this.editStr.set(v);
      } else if (this.editList != null) {
         if (this.editListIdx < 0) {
            this.editList.addEntry(v);
         } else {
            this.editList.setEntry(this.editListIdx, v);
         }
      }
      ConfigManager.save();
      this.closeEditor();
   }

   private void closeEditor() {
      this.editStr = null;
      this.editList = null;
      this.editListIdx = -1;
      this.editor = null;
      this.method_41843();
   }

   private String playerName() {
      class_310 m = class_310.method_1551();
      return m.field_1724 != null ? m.field_1724.method_7334().name() : "Player";
   }

   // --- rounded rect ------------------------------------------------------

   private static void fillRound(class_332 ctx, int x, int y, int w, int h, int r, int color) {
      if (w <= 0 || h <= 0) {
         return;
      }
      r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
      if (r == 0) {
         ctx.method_25294(x, y, x + w, y + h, color);
         return;
      }
      ctx.method_25294(x, y + r, x + w, y + h - r, color);
      for (int dy = 0; dy < r; ++dy) {
         int v = r - dy;
         int inset = r - (int) Math.round(Math.sqrt((double) (r * r - v * v)));
         ctx.method_25294(x + inset, y + dy, x + w - inset, y + dy + 1, color);
         ctx.method_25294(x + inset, y + h - 1 - dy, x + w - inset, y + h - dy, color);
      }
   }

   public boolean method_25421() {
      return false;
   }
}
