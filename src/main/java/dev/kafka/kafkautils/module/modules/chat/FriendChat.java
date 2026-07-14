package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.ChatUtil;
import java.util.List;
import net.minecraft.class_3414;
import net.minecraft.class_3417;

/**
 * Private chat between friends who also run this mod. Type {@code // message} to
 * send: the mod whispers an encoded line to every online friend via the server's
 * private-message command. On the receiving side the mod recognises the encoded
 * line (from someone in <b>their</b> friend list) and shows it as friend chat,
 * hiding the raw whisper. Two players see each other's messages when they have
 * each other added; 2+ mutual friends effectively form a group.
 *
 * <p>NOTE: relies on the server having a working private-message command (set in
 * "Msg Command", default "msg") and not filtering the marker. It is not encrypted
 * — treat it as convenience, not privacy.
 */
public class FriendChat extends Module {
   private static final String MARKER = "§r§k#§r"; // recognisable, near-invisible tag
   private static final String PLAIN_MARKER = ">>kf>>"; // fallback marker that survives strict chat filters

   private final StringSetting msgCommand = this.add(new StringSetting("Msg Command", "msg"));
   private final BooleanSetting usePlainMarker = this.add(new BooleanSetting("Plain Marker", true));
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));

   public FriendChat() {
      super("Friend Chat", "Private friend chat: type // message.", Category.FRIENDS);
   }

   private String marker() {
      return this.usePlainMarker.get() ? PLAIN_MARKER : MARKER;
   }

   /** Handles an outgoing chat line. @return true if it was a // friend message (cancel normal send). */
   public boolean handleOutgoing(String raw) {
      if (!this.isEnabled() || raw == null || !raw.startsWith("//")) {
         return false;
      }
      String text = raw.substring(2).trim();
      if (text.isEmpty() || mc.field_1724 == null || mc.method_1562() == null) {
         return true;
      }
      FriendList list = ModuleManager.get(FriendList.class);
      List<String> online = list != null ? list.onlineFriends() : List.of();
      String me = mc.field_1724.method_7334().name();
      String payload = this.marker() + me + "|" + text;

      int sent = 0;
      for (String friend : online) {
         mc.method_1562().method_45730(this.msgCommand.get() + " " + friend + " " + payload);
         ++sent;
      }
      ChatUtil.raw("§d[Друзья] §rты§7: §r" + text + (sent == 0 ? " §8(нет друзей онлайн)" : ""));
      return true;
   }

   /** Handles an incoming server message. @return true if it was a friend message (hide the raw line). */
   public boolean handleIncoming(String rawText) {
      if (!this.isEnabled() || rawText == null) {
         return false;
      }
      int idx = rawText.indexOf(this.marker());
      if (idx < 0) {
         return false;
      }
      String payload = rawText.substring(idx + this.marker().length());
      int bar = payload.indexOf('|');
      if (bar < 0) {
         return false;
      }
      String sender = payload.substring(0, bar).trim();
      String text = payload.substring(bar + 1);
      FriendList list = ModuleManager.get(FriendList.class);
      if (list == null || !list.isFriend(sender)) {
         return false; // only accept from people we have added
      }
      ChatUtil.raw("§d[Друзья] §b" + sender + "§7: §r" + text);
      if (this.sound.get() && mc.field_1724 != null) {
         mc.field_1724.method_5783((class_3414)class_3417.field_14622.comp_349(), 0.6F, 1.4F);
      }
      return true;
   }
}
