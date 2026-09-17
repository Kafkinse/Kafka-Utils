package dev.kafka.kafkautils.chatplus;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * F8 chat manager, 26.2. All layout/state lives in {@link ChatPlusScreenLogic}.
 * Mouse-wheel scrolling isn't wired up yet on this target — 26.2's
 * mouseScrolled signature isn't verified against real source yet, so it's
 * left for a follow-up rather than guessed.
 */
public final class ChatPlusScreen extends Screen {
   private final ChatPlusScreenLogic logic = new ChatPlusScreenLogic();

   public ChatPlusScreen() {
      super(Component.literal("Kafka Chat+"));
   }

   @Override
   protected void init() {
      int w = 150;
      EditBox search = new EditBox(this.font, this.width - w - 10, 6, w, 16, Component.literal("поиск"));
      search.setHint(Component.literal("поиск по чату…"));
      search.setResponder(value -> this.logic.setSearchQuery(value));
      this.addRenderableWidget(search);
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      this.logic.render(new CompatGraphics(graphics, this.font), mouseX, mouseY, this.width, this.height);
      super.extractRenderState(graphics, mouseX, mouseY, delta);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (event.button() == 0 && this.logic.mouseClicked(event.x(), event.y(), this.width, this.height)) {
         if (this.logic.closeRequested()) {
            this.onClose();
         }
         return true;
      }
      return super.mouseClicked(event, doubleClick);
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
         this.onClose();
         return true;
      }
      return super.keyPressed(event);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
