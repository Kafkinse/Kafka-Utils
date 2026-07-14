package dev.kafka.kafkautils.module.modules.chat;

import dev.kafka.kafkautils.module.Category;
import dev.kafka.kafkautils.module.HudModule;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.setting.BooleanSetting;
import dev.kafka.kafkautils.setting.StringSetting;
import dev.kafka.kafkautils.util.RenderUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.class_268;
import net.minecraft.class_269;
import net.minecraft.class_332;
import net.minecraft.class_640;

/**
 * Your clan roster, shared by every clan module (Clan Chat Highlight, Team
 * Overlay, …).
 *
 * <p>On a Simple Clans server the plugin puts clan members on a shared scoreboard
 * team (that's what colours their names/tags), so with "Auto (Simple Clans)" on,
 * membership is derived automatically from your own team — no manual list needed.
 * The optional Members field adds extra names on top (allies, etc.).
 */
public class ClanList extends Module implements HudModule {
   private final BooleanSetting autoTeam = this.add(new BooleanSetting("Auto (Simple Clans)", true));
   private final StringSetting nicks = this.add(new StringSetting("Extra Members", ""));

   public ClanList() {
      super("Clan List", "Clan roster (auto from Simple Clans team + optional extra names).", Category.CLAN);
   }

   private Set<String> manualMembers() {
      Set<String> out = new HashSet<>();
      for (String part : this.nicks.get().split("[,\\s]+")) {
         if (!part.isBlank()) {
            out.add(part.trim().toLowerCase(Locale.ROOT));
         }
      }
      return out;
   }

   /** @return the scoreboard-team name the local player belongs to, or null. */
   private String myTeamName() {
      if (!this.autoTeam.get() || mc.field_1724 == null) {
         return null;
      }
      class_268 team = mc.field_1724.method_5781();
      return team != null ? team.method_1197() : null;
   }

   /** @return true if the named player shares the local player's clan team. */
   private boolean sameTeam(String name) {
      String mine = this.myTeamName();
      if (mine == null || mc.field_1687 == null) {
         return false;
      }
      class_269 scoreboard = mc.field_1687.method_8428();
      class_268 team = scoreboard.method_1164(name);
      return team != null && mine.equals(team.method_1197());
   }

   public boolean isMember(String name) {
      if (name == null) {
         return false;
      }
      return this.manualMembers().contains(name.toLowerCase(Locale.ROOT)) || this.sameTeam(name);
   }

   /** @return the effective roster (manual names + online team-mates), lower-cased. */
   public Set<String> members() {
      Set<String> out = new HashSet<>(this.manualMembers());
      if (this.autoTeam.get() && mc.method_1562() != null) {
         for (class_640 entry : mc.method_1562().method_2880()) {
            if (entry.method_2966() != null) {
               String name = entry.method_2966().name();
               if (this.sameTeam(name)) {
                  out.add(name.toLowerCase(Locale.ROOT));
               }
            }
         }
      }
      return out;
   }

   /** @return names of clan members currently visible in the tab list. */
   public List<String> onlineMembers() {
      List<String> out = new ArrayList<>();
      if (mc.method_1562() == null) {
         return out;
      }
      for (class_640 entry : mc.method_1562().method_2880()) {
         if (entry.method_2966() != null) {
            String name = entry.method_2966().name();
            if (this.isMember(name)) {
               out.add(name);
            }
         }
      }
      return out;
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList<>();
      List<String> online = onlineMembers();
      String team = this.myTeamName();
      lines.add("§7Клан: §d" + (team != null ? team : "—") + " §7| онлайн: §a" + online.size());
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
