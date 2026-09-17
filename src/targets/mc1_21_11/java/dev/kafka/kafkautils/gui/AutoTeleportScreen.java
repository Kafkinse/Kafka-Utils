package dev.kafka.kafkautils.gui;

import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.module.modules.chat.AutoTeleport;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;

/**
 * Manage the Auto Teleport allow-list: click an online player on the left to
 * allow them, click an allowed player on the right to remove them, or type a
 * name to add someone who is offline. Opened by toggling the Auto Teleport
 * module or with {@code /kafka tpa}.
 */
public class AutoTeleportScreen extends class_437 {
   private static final int BG = 0xE6101014;
   private static final int HEADER = 0xFF1B1030;
   private static final int ACCENT = 0xFFB388FF;
   private static final int PANEL = 0x33000000;
   private static final int COL2 = 200;

   private final AutoTeleport tp;
   private class_342 addField;

   public AutoTeleportScreen() {
      super(class_2561.method_43470("Авто-телепорт"));
      this.tp = ModuleManager.get(AutoTeleport.class);
   }

   protected void method_25426() {
      if (this.tp == null) {
         return;
      }

      // Manual add field + button.
      this.addField = new class_342(this.field_22793, 16, 40, 140, 16, class_2561.method_43470("Ник"));
      this.addField.method_1880(32);
      this.addField.method_47404(class_2561.method_43470("§7ник игрока…"));
      this.method_37063(this.addField);
      this.method_37063(class_4185.method_46430(class_2561.method_43470("§aДобавить"), (b) -> {
         String n = this.addField.method_1882().trim();
         if (!n.isEmpty()) {
            this.tp.add(n);
            this.addField.method_1852("");
            this.method_41843();
         }
      }).method_46434(160, 40, 70, 16).method_46431());

      // Left: online players (click to allow).
      int y = 76;
      for (String name : this.tp.onlinePlayers()) {
         final String n = name;
         boolean on = this.tp.isAllowed(name);
         this.method_37063(class_4185.method_46430(class_2561.method_43470((on ? "§a✔ " : "§7+ ") + name), (b) -> {
            this.tp.add(n);
            this.method_41843();
         }).method_46434(16, y, 176, 14).method_46431());
         y += 16;
      }

      // Right: allowed players (click to remove).
      int ry = 76;
      for (String name : this.tp.allowedList()) {
         final String n = name;
         this.method_37063(class_4185.method_46430(class_2561.method_43470("§c✖ §r" + name), (b) -> {
            this.tp.remove(n);
            this.method_41843();
         }).method_46434(COL2, ry, 176, 14).method_46431());
         ry += 16;
      }
   }

   public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
      int w = this.field_22789;
      int h = this.field_22790;
      ctx.method_25294(0, 0, w, h, BG);
      ctx.method_25294(0, 0, w, 30, HEADER);
      ctx.method_25294(0, 29, w, 31, ACCENT);
      ctx.method_51433(this.field_22793, "§5§lАвто §d§l— телепорт", 16, 11, 0xFFD9C2FF, true);
      ctx.method_51433(this.field_22793, "§7ESC", w - 32, 11, 0xFF9A8FB0, true);

      ctx.method_25294(12, 62, COL2 - 8, h - 12, PANEL);
      ctx.method_25294(COL2 - 4, 62, w - 12, h - 12, PANEL);

      super.method_25394(ctx, mouseX, mouseY, delta);

      ctx.method_51433(this.field_22793, "§7Онлайн — нажми, чтобы разрешить", 16, 62, 0xFF9A8FB0, true);
      ctx.method_51433(this.field_22793, "§7Разрешённые — нажми, чтобы убрать", COL2, 62, 0xFF9A8FB0, true);
   }

   public boolean method_25421() {
      return false;
   }
}
