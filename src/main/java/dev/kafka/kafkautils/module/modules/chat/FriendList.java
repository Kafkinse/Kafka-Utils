package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.config.ConfigManager;
import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.class_332;
import net.minecraft.class_640;

/**
 * Central friends roster, shared by every friend module (Friend Chat, Friend
 * Highlight, Friend Overlay). Add/remove friends with {@code /kafka team add
 * <name>} / {@code /kafka team remove <name>} or by editing the Friends field.
 */
public class FriendList extends Module implements HudModule {
   private final StringSetting friendsSetting = this.add(new StringSetting("Friends", ""));

   public FriendList() {
      super("Friend List", "Your friends list (/kafka team add <name>).", Category.FRIENDS);
   }

   /** @return all friend names, lower-cased, order preserved. */
   public Set<String> friends() {
      Set<String> out = new LinkedHashSet<>();
      for (String part : this.friendsSetting.get().split("[,\\s]+")) {
         if (!part.isBlank()) {
            out.add(part.trim().toLowerCase(Locale.ROOT));
         }
      }
      return out;
   }

   public boolean isFriend(String name) {
      return name != null && this.friends().contains(name.toLowerCase(Locale.ROOT));
   }

   public boolean addFriend(String name) {
      if (name == null || name.isBlank() || this.isFriend(name)) {
         return false;
      }
      Set<String> f = this.friends();
      f.add(name.trim().toLowerCase(Locale.ROOT));
      this.save(f);
      return true;
   }

   public boolean removeFriend(String name) {
      if (name == null || !this.isFriend(name)) {
         return false;
      }
      Set<String> f = this.friends();
      f.remove(name.toLowerCase(Locale.ROOT));
      this.save(f);
      return true;
   }

   private void save(Set<String> f) {
      this.friendsSetting.set(String.join(", ", f));
      ConfigManager.save();
   }

   /** @return profile names of friends currently in the tab list. */
   public List<String> onlineFriends() {
      List<String> out = new ArrayList<>();
      if (mc.method_1562() == null) {
         return out;
      }
      Set<String> f = this.friends();
      for (class_640 entry : mc.method_1562().method_2880()) {
         if (entry.method_2966() != null) {
            String name = entry.method_2966().name();
            if (f.contains(name.toLowerCase(Locale.ROOT))) {
               out.add(name);
            }
         }
      }
      return out;
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList<>();
      List<String> online = this.onlineFriends();
      lines.add("§7Друзей: §a" + this.friends().size() + " §7| онлайн: §a" + online.size());
      if (online.isEmpty()) {
         lines.add("§7никого нет");
      } else {
         for (String n : online) {
            lines.add("§a" + n);
         }
      }
      return RenderUtil.panel(ctx, x, y, "Friends", lines);
   }
}
