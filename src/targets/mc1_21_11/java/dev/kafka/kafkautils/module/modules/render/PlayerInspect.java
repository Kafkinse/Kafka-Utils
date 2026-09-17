package dev.kafka.kafkautils.module.modules.render;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.util.RenderUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.class_1291;
import net.minecraft.class_1293;
import net.minecraft.class_1297;
import net.minecraft.class_1304;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_239;
import net.minecraft.class_332;
import net.minecraft.class_3966;
import net.minecraft.class_6880;
import net.minecraft.class_9304;
import net.minecraft.class_239.class_240;

public class PlayerInspect extends Module implements HudModule {
   private static final class_1304[] SLOTS;
   private final BooleanSetting showEnchants = (BooleanSetting)this.add(new BooleanSetting("Enchants", true));
   private final BooleanSetting compact = (BooleanSetting)this.add(new BooleanSetting("Compact ench", true));
   private final BooleanSetting showHands = (BooleanSetting)this.add(new BooleanSetting("Hands", true));
   private final BooleanSetting showEffects = (BooleanSetting)this.add(new BooleanSetting("Effects", true));

   public PlayerInspect() {
      super("Player Inspect", "Shows armor & enchants of the player you aim at.", Category.RENDER);
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      if (mc.field_1765 != null && mc.field_1765.method_17783() == class_240.field_1331) {
         class_239 var5 = mc.field_1765;
         if (!(var5 instanceof class_3966)) {
            return new int[]{0, 0};
         } else {
            class_3966 ehr = (class_3966)var5;
            class_1297 var6 = ehr.method_17782();
            if (!(var6 instanceof class_1657)) {
               return new int[]{0, 0};
            } else {
               class_1657 target = (class_1657)var6;
               ArrayList var17 = new ArrayList();
               boolean any = false;

               for(class_1304 slot : SLOTS) {
                  if (slot != class_1304.field_6173 && slot != class_1304.field_6171 || this.showHands.get()) {
                     class_1799 stack = target.method_6118(slot);
                     if (!stack.method_7960()) {
                        any = true;
                        String var10001 = slotName(slot);
                        var17.add("§d" + var10001 + ": §r" + stack.method_7964().getString());
                        class_9304 ench = stack.method_58657();
                        if (this.showEnchants.get() && !ench.method_57543()) {
                           for(Object2IntMap.Entry<class_6880<class_1887>> e : ench.method_57539()) {
                              var10001 = this.enchLabel((class_6880)e.getKey(), e.getIntValue());
                              var17.add("§7  " + var10001);
                           }
                        }
                     }
                  }
               }

               if (!any) {
                  var17.add("§7нет экипировки");
               }

               if (this.showEffects.get()) {
                  Collection<class_1293> effects = target.method_6026();
                  if (!effects.isEmpty()) {
                     var17.add("§dЭффекты:");

                     for(class_1293 e : effects) {
                        String n = ((class_1291)e.method_5579().comp_349()).method_5560().getString();
                        String dur = e.method_48559() ? "∞" : RenderUtil.time(e.method_5584() / 20);
                        var17.add("§7  " + n + " " + RenderUtil.roman(e.method_5578() + 1) + " (" + dur + ")");
                     }
                  } else {
                     var17.add("§7Эффекты: не видны (сервер не шлёт)");
                  }
               }

               return RenderUtil.panel(ctx, x, y, "Осмотр: " + target.method_5477().getString(), var17);
            }
         }
      } else {
         return new int[]{0, 0};
      }
   }

   private String enchLabel(class_6880<class_1887> ench, int lvl) {
      if (!this.compact.get()) {
         return class_1887.method_8179(ench, lvl).getString();
      } else {
         String base = ((class_1887)ench.comp_349()).comp_2686().getString();
         String ab = base.length() > 4 ? base.substring(0, 4) + "." : base;
         return ab + " " + lvl;
      }
   }

   private static String slotName(class_1304 slot) {
      String var10000;
      switch (slot) {
         case field_6169 -> var10000 = "Шлем";
         case field_6174 -> var10000 = "Нагрудник";
         case field_6172 -> var10000 = "Поножи";
         case field_6166 -> var10000 = "Ботинки";
         case field_6173 -> var10000 = "Рука";
         case field_6171 -> var10000 = "Офф-рука";
         default -> var10000 = slot.method_5923();
      }

      return var10000;
   }

   static {
      SLOTS = new class_1304[]{class_1304.field_6169, class_1304.field_6174, class_1304.field_6172, class_1304.field_6166, class_1304.field_6173, class_1304.field_6171};
   }
}
