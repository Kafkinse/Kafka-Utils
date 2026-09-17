package dev.kafka.kafkautils.chatplus;

import dev.kafka.kafkautils.util.ChatUtil;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

/** F8 chat manager, 1.21.11. All layout/state lives in {@link ChatPlusScreenLogic}. */
public final class ChatPlusScreen extends class_437 {
   private final ChatPlusScreenLogic logic = new ChatPlusScreenLogic();
   private class_342 search;

   public ChatPlusScreen() {
      super(class_2561.method_43470("Kafka Chat+"));
   }

   protected void method_25426() {
      int w = 150;
      String prev = this.search != null ? this.search.method_1882() : "";
      this.search = new class_342(this.field_22793, this.field_22789 - w - 10, 6, w, 16,
            class_2561.method_43470("поиск"));
      this.search.method_1858(false);
      this.search.method_1868(0xFFE7DAF6);
      this.search.method_47404(class_2561.method_43470("§7поиск по чату…"));
      this.search.method_1852(prev);
      this.method_37063(this.search);
   }

   public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
      this.logic.setSearchQuery(this.search != null ? this.search.method_1882() : "");
      this.logic.render(new CompatGraphics(ctx, this.field_22793), mouseX, mouseY, this.field_22789, this.field_22790);
      super.method_25394(ctx, mouseX, mouseY, delta);
   }

   public boolean method_25402(class_11909 click, boolean doubled) {
      int button = click.method_74245();
      if ((button == 0 || button == 1)
            && this.logic.mouseClicked(click.comp_4798(), click.comp_4799(), button, this.field_22789, this.field_22790)) {
         if (this.logic.closeRequested()) {
            this.method_25419();
         }
         String copyText = this.logic.consumeCopyRequest();
         if (copyText != null) {
            class_310.method_1551().field_1774.method_1455(copyText);
            ChatUtil.info("§7скопировано в буфер обмена.");
         }
         return true;
      }
      return super.method_25402(click, doubled);
   }

   public boolean method_25401(double mouseX, double mouseY, double horiz, double vert) {
      if (vert != 0) {
         this.logic.mouseScrolled(vert);
         return true;
      }
      return super.method_25401(mouseX, mouseY, horiz, vert);
   }

   public boolean method_25404(class_11908 key) {
      if (key.comp_4795() == GLFW.GLFW_KEY_ESCAPE) {
         this.method_25419();
         return true;
      }
      return super.method_25404(key);
   }

   public boolean method_25421() {
      return false;
   }
}
