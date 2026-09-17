package dev.kafka.kafkautils.chatplus;

import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

/** F8 chat manager, 1.21.11. All layout/state lives in {@link ChatPlusScreenLogic}. */
public final class ChatPlusScreen extends class_437 {
   private final ChatPlusScreenLogic logic = new ChatPlusScreenLogic();

   public ChatPlusScreen() {
      super(class_2561.method_43470("Kafka Chat+"));
   }

   public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
      this.logic.render(new CompatGraphics(ctx, this.field_22793), mouseX, mouseY, this.field_22789, this.field_22790);
      super.method_25394(ctx, mouseX, mouseY, delta);
   }

   public boolean method_25402(class_11909 click, boolean doubled) {
      if (click.method_74245() == 0
            && this.logic.mouseClicked(click.comp_4798(), click.comp_4799(), this.field_22789, this.field_22790)) {
         if (this.logic.closeRequested()) {
            this.method_25419();
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
