package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.class_332;
import net.minecraft.class_640;

/**
 * Your clan roster. Stores a list of member nicknames and exposes membership
 * queries used by the other clan features (Clan Chat Highlight, Team Overlay).
 * When enabled it also shows how many members are currently online.
 */
public class ClanList extends Module implements HudModule {
   private final StringSetting nicks = this.add(new StringSetting("Members", ""));

   public ClanList() {
      super("Clan List", "Clan roster (comma/space separated) used by the other clan modules.", Category.CLAN);
   }

   /** @return all configured member names, lower-cased. */
   public Set<String> members() {
      Set<String> out = new HashSet<>();
      for (String part : this.nicks.get().split("[,\\s]+")) {
         if (!part.isBlank()) {
            out.add(part.trim().toLowerCase(Locale.ROOT));
         }
      }
      return out;
   }

   public boolean isMember(String name) {
      return name != null && members().contains(name.toLowerCase(Locale.ROOT));
   }

   /** @return names of clan members currently visible in the tab list. */
   public List<String> onlineMembers() {
      List<String> out = new ArrayList<>();
      if (mc.method_1562() == null) {
         return out;
      }
      Set<String> members = members();
      for (class_640 entry : mc.method_1562().method_2880()) {
         if (entry.method_2966() != null) {
            String name = entry.method_2966().name();
            if (members.contains(name.toLowerCase(Locale.ROOT))) {
               out.add(name);
            }
         }
      }
      return out;
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList<>();
      List<String> online = onlineMembers();
      lines.add("§7Онлайн: §a" + online.size() + " §7/ " + members().size());
      if (online.isEmpty()) {
         lines.add("§7никого нет");
      } else {
         for (String n : online) {
            lines.add("§a" + n);
         }
      }
      return RenderUtil.panel(ctx, x, y, "Clan", lines);
   }
}
