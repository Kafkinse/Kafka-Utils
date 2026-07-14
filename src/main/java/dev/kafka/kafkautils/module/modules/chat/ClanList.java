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
import java.util.regex.Pattern;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_640;

/**
 * Your clan roster, shared by every clan module (Clan Chat Highlight, Team
 * Overlay, …).
 *
 * <p>Simple Clans shows each member's clan tag in the tab list (to the right of
 * the nick). Set your tag in <b>Clan Tag</b> (e.g. OLD) and everyone whose tab
 * entry carries that tag is treated as a clan-mate. Leave it empty and, with
 * <b>Auto Tag</b> on, it reads the tag from your own tab entry automatically.
 * The optional Extra Members field adds names on top (allies, etc.).
 */
public class ClanList extends Module implements HudModule {
   private final StringSetting clanTag = this.add(new StringSetting("Clan Tag", ""));
   private final BooleanSetting autoTag = this.add(new BooleanSetting("Auto Tag", true));
   private final StringSetting nicks = this.add(new StringSetting("Extra Members", ""));

   public ClanList() {
      super("Clan List", "Clan roster from the Simple Clans tab tag (manual or auto) + extra names.", Category.CLAN);
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

   private String displayOf(class_640 entry) {
      class_2561 dn = entry.method_2971();
      if (dn != null) {
         return strip(dn.getString());
      }
      return entry.method_2966() != null ? entry.method_2966().name() : "";
   }

   /** The effective clan tag: the manual value, or the one auto-read from your tab entry. */
   private String effectiveTag() {
      String manual = this.clanTag.get().trim();
      if (!manual.isEmpty()) {
         return manual;
      }
      if (!this.autoTag.get() || mc.field_1724 == null || mc.method_1562() == null) {
         return "";
      }
      UUID myId = mc.field_1724.method_5667();
      for (class_640 entry : mc.method_1562().method_2880()) {
         if (entry.method_2966() != null && myId.equals(entry.method_2966().id())) {
            String display = this.displayOf(entry);
            String name = entry.method_2966().name();
            int idx = display.indexOf(name);
            return idx >= 0 ? display.substring(idx + name.length()).trim() : "";
         }
      }
      return "";
   }

   private boolean displayHasTag(class_640 entry, Pattern tagPattern) {
      return tagPattern != null && tagPattern.matcher(this.displayOf(entry)).find();
   }

   private Pattern tagPattern(String tag) {
      return tag.isEmpty() ? null : Pattern.compile("(?i)\\b" + Pattern.quote(tag) + "\\b");
   }

   public boolean isMember(String name) {
      if (name == null) {
         return false;
      }
      if (this.manualMembers().contains(name.toLowerCase(Locale.ROOT))) {
         return true;
      }
      Pattern p = this.tagPattern(this.effectiveTag());
      if (p == null || mc.method_1562() == null) {
         return false;
      }
      for (class_640 entry : mc.method_1562().method_2880()) {
         if (entry.method_2966() != null && entry.method_2966().name().equalsIgnoreCase(name)) {
            return this.displayHasTag(entry, p);
         }
      }
      return false;
   }

   /** @return the effective roster (manual names + online clan-mates), lower-cased. */
   public Set<String> members() {
      Set<String> out = new HashSet<>(this.manualMembers());
      Pattern p = this.tagPattern(this.effectiveTag());
      if (p != null && mc.method_1562() != null) {
         for (class_640 entry : mc.method_1562().method_2880()) {
            if (entry.method_2966() != null && this.displayHasTag(entry, p)) {
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
      Pattern p = this.tagPattern(this.effectiveTag());
      Set<String> manual = this.manualMembers();
      for (class_640 entry : mc.method_1562().method_2880()) {
         if (entry.method_2966() != null) {
            String name = entry.method_2966().name();
            if (manual.contains(name.toLowerCase(Locale.ROOT)) || this.displayHasTag(entry, p)) {
               out.add(name);
            }
         }
      }
      return out;
   }

   public int[] onHudRender(class_332 ctx, int x, int y) {
      List<String> lines = new ArrayList<>();
      List<String> online = onlineMembers();
      String tag = this.effectiveTag();
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
