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
import java.util.UUID;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_640;

/**
 * Your clan roster, shared by every clan module (Clan Chat Highlight, Team
 * Overlay, …).
 *
 * <p>Simple Clans shows each member's clan tag in the tab list, to the right of
 * the nick (e.g. "Kirill [KAFKA]"). With "Auto (Simple Clans)" on, this reads the
 * tag from your own tab entry and treats everyone whose tab entry carries the
 * same tag as a clan-mate — no manual list needed. The optional Extra Members
 * field adds names on top (allies, etc.).
 */
public class ClanList extends Module implements HudModule {
   private final BooleanSetting autoTag = this.add(new BooleanSetting("Auto (Simple Clans)", true));
   private final StringSetting nicks = this.add(new StringSetting("Extra Members", ""));

   public ClanList() {
      super("Clan List", "Clan roster (auto from the Simple Clans tab tag + optional extra names).", Category.CLAN);
   }

   private static String strip(String s) {
      return s == null ? "" : s.replaceAll("(?i)§[0-9A-FK-OR]", "").trim();
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

   /** The clan tag shown to the right of a player's nick in their tab entry. */
   private String tagOf(class_640 entry) {
      if (entry.method_2966() == null) {
         return "";
      }
      class_2561 dn = entry.method_2971();
      String display = strip(dn != null ? dn.getString() : entry.method_2966().name());
      String name = entry.method_2966().name();
      int idx = display.indexOf(name);
      return idx >= 0 ? display.substring(idx + name.length()).trim() : "";
   }

   /** @return the local player's clan tag, or "" if none/disabled. */
   private String myTag() {
      if (!this.autoTag.get() || mc.field_1724 == null || mc.method_1562() == null) {
         return "";
      }
      UUID myId = mc.field_1724.method_5667();
      for (class_640 entry : mc.method_1562().method_2880()) {
         if (entry.method_2966() != null && myId.equals(entry.method_2966().id())) {
            return tagOf(entry);
         }
      }
      return "";
   }

   private boolean sameClan(String name, String myTag) {
      if (myTag.isEmpty() || mc.method_1562() == null) {
         return false;
      }
      for (class_640 entry : mc.method_1562().method_2880()) {
         if (entry.method_2966() != null && entry.method_2966().name().equalsIgnoreCase(name)) {
            return myTag.equals(tagOf(entry));
         }
      }
      return false;
   }

   public boolean isMember(String name) {
      if (name == null) {
         return false;
      }
      return this.manualMembers().contains(name.toLowerCase(Locale.ROOT)) || this.sameClan(name, this.myTag());
   }

   /** @return the effective roster (manual names + online clan-mates), lower-cased. */
   public Set<String> members() {
      Set<String> out = new HashSet<>(this.manualMembers());
      String tag = this.myTag();
      if (!tag.isEmpty() && mc.method_1562() != null) {
         for (class_640 entry : mc.method_1562().method_2880()) {
            if (entry.method_2966() != null && tag.equals(tagOf(entry))) {
               out.add(entry.method_2966().name().toLowerCase(Locale.ROOT));
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
      String tag = this.myTag();
      Set<String> manual = this.manualMembers();
      for (class_640 entry : mc.method_1562().method_2880()) {
         if (entry.method_2966() != null) {
            String name = entry.method_2966().name();
            if (manual.contains(name.toLowerCase(Locale.ROOT)) || (!tag.isEmpty() && tag.equals(tagOf(entry)))) {
               out.add(name);
            }
         }
      }
      return out;
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList<>();
      List<String> online = onlineMembers();
      String tag = this.myTag();
      lines.add("§7Тег: §d" + (tag.isEmpty() ? "—" : tag) + " §7| онлайн: §a" + online.size());
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
