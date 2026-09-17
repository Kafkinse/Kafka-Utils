package dev.kafka.kafkautils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.kafka.kafkautils.hud.HudManager;
import dev.kafka.kafkautils.module.Module;
import dev.kafka.kafkautils.module.ModuleManager;
import dev.kafka.kafkautils.setting.Setting;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
   private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("kafkautils.json");
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();

   private ConfigManager() {
   }

   public static void load() {
      try {
         if (!Files.exists(PATH, new LinkOption[0])) {
            return;
         }

         JsonObject root = (JsonObject)GSON.fromJson(Files.readString(PATH), JsonObject.class);
         if (root == null) {
            return;
         }

         JsonObject mods = root.getAsJsonObject("modules");
         if (mods != null) {
            for(Module m : ModuleManager.getModules()) {
               JsonObject mo = mods.getAsJsonObject(m.getName());
               if (mo != null) {
                  if (mo.has("enabled")) {
                     m.setEnabled(mo.get("enabled").getAsBoolean());
                  }

                  JsonObject s = mo.getAsJsonObject("settings");
                  if (s != null) {
                     for(Setting set : m.getSettings()) {
                        if (s.has(set.getKey())) {
                           set.deserialize(s.get(set.getKey()).getAsString());
                        }
                     }
                  }
               }
            }
         }

         JsonObject hud = root.getAsJsonObject("hud");
         if (hud != null) {
            for(String name : hud.keySet()) {
               JsonArray a = hud.getAsJsonArray(name);
               if (a != null && a.size() == 2) {
                  HudManager.setPosition(name, a.get(0).getAsInt(), a.get(1).getAsInt());
               }
            }
         }
      } catch (Exception e) {
         System.err.println("[KafkaUtils] Failed to load config: " + e.getMessage());
      }

   }

   public static void save() {
      try {
         JsonObject root = new JsonObject();
         JsonObject mods = new JsonObject();

         for(Module m : ModuleManager.getModules()) {
            JsonObject mo = new JsonObject();
            mo.addProperty("enabled", m.isEnabled());
            JsonObject s = new JsonObject();

            for(Setting set : m.getSettings()) {
               s.addProperty(set.getKey(), set.serialize());
            }

            mo.add("settings", s);
            mods.add(m.getName(), mo);
         }

         root.add("modules", mods);
         JsonObject hud = new JsonObject();

         for(Map.Entry<String, int[]> e : HudManager.getPositions().entrySet()) {
            JsonArray a = new JsonArray();
            a.add(((int[])e.getValue())[0]);
            a.add(((int[])e.getValue())[1]);
            hud.add((String)e.getKey(), a);
         }

         root.add("hud", hud);
         Files.writeString(PATH, GSON.toJson(root));
      } catch (Exception e) {
         System.err.println("[KafkaUtils] Failed to save config: " + e.getMessage());
      }

   }
}
