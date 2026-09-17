package dev.kafka.kafkautils.chatplus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Client side of the vnbx:bridge plugin channel — a server-side plugin that
 * answers clan/marriage lookups over a custom packet instead of parsing chat
 * text. Whether vanilla-box.ru's server actually runs this plugin is
 * unverified from here; if it doesn't, every request just times out after
 * 5s with an "unavailable" result — harmless either way. Pure Java (Gson,
 * already a Minecraft library dependency) — no Minecraft type touched, so
 * this is shared; only the actual packet send/receive is per-target.
 */
public final class VnbxBridgeClient {
   private static final int PROTOCOL_VERSION = 1;
   private static final int MAX_PAYLOAD_BYTES = 16 * 1024;
   private static final long REQUEST_TIMEOUT_MS = 5_000L;
   private static final Map<String, PendingRequest> pending = new HashMap<>();

   private VnbxBridgeClient() {
   }

   public static byte[] request(String type) {
      JsonObject message = new JsonObject();
      message.addProperty("protocol", PROTOCOL_VERSION);
      message.addProperty("type", type);
      return message.toString().getBytes(StandardCharsets.UTF_8);
   }

   public static byte[] playerRelationsRequest(String requestId, String player) {
      JsonObject message = new JsonObject();
      message.addProperty("protocol", PROTOCOL_VERSION);
      message.addProperty("type", "request_player_relations");
      message.addProperty("requestId", requestId);
      message.addProperty("player", player);
      return message.toString().getBytes(StandardCharsets.UTF_8);
   }

   public static CompletableFuture<VnbxRelations> requestPlayerRelations(String player, Consumer<byte[]> sender) {
      String requestId = UUID.randomUUID().toString();
      CompletableFuture<VnbxRelations> future = new CompletableFuture<>();
      synchronized (pending) {
         pending.put(requestId, new PendingRequest(player, future));
      }
      try {
         sender.accept(playerRelationsRequest(requestId, player));
      } catch (RuntimeException e) {
         complete(requestId, VnbxRelations.unavailable(player));
         return future;
      }
      CompletableFuture.delayedExecutor(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .execute(() -> complete(requestId, VnbxRelations.unavailable(player)));
      return future;
   }

   /** Feeds one incoming vnbx:bridge packet in. Returns true if it was a recognised, well-formed message. */
   public static boolean receive(byte[] payload) {
      if (payload == null || payload.length == 0 || payload.length > MAX_PAYLOAD_BYTES) {
         return false;
      }
      try {
         String json = new String(payload, StandardCharsets.UTF_8);
         JsonObject message = JsonParser.parseString(json).getAsJsonObject();
         if (message.get("protocol") == null || message.get("protocol").getAsInt() != PROTOCOL_VERSION) {
            return false;
         }
         String type = message.has("type") ? message.get("type").getAsString() : null;
         if (!"player_relations".equals(type)) {
            return true; // a recognised envelope we just don't act on (e.g. a "hello" ack)
         }
         String requestId = message.get("requestId").getAsString();
         String player = message.get("player").getAsString();
         JsonObject clan = message.has("clan") ? message.getAsJsonObject("clan") : null;
         JsonObject marriage = message.has("marriage") ? message.getAsJsonObject("marriage") : null;
         boolean clanAvailable = getBool(message, "clanAvailable");
         boolean marriageAvailable = getBool(message, "marriageAvailable");
         boolean inClan = clan != null && getBool(clan, "inClan");
         String tag = clan != null ? getString(clan, "tag") : null;
         String name = clan != null ? getString(clan, "name") : null;
         String rank = clan != null ? getString(clan, "rank") : null;
         boolean married = marriage != null && getBool(marriage, "married");
         String partner = marriage != null ? getString(marriage, "partnerName") : null;
         VnbxRelations relations = new VnbxRelations(clanAvailable || marriageAvailable, player,
               inClan, tag, name, rank, married, partner);
         complete(requestId, relations);
         return true;
      } catch (RuntimeException e) {
         return false;
      }
   }

   public static void reset() {
      Map<String, PendingRequest> copy;
      synchronized (pending) {
         copy = new HashMap<>(pending);
         pending.clear();
      }
      copy.forEach((id, req) -> req.future().complete(VnbxRelations.unavailable(req.player())));
   }

   private static void complete(String requestId, VnbxRelations relations) {
      PendingRequest req;
      synchronized (pending) {
         req = pending.remove(requestId);
      }
      if (req != null) {
         req.future().complete(relations);
      }
   }

   private static boolean getBool(JsonObject o, String key) {
      return o.has(key) && o.get(key).getAsBoolean();
   }

   private static String getString(JsonObject o, String key) {
      return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
   }

   private record PendingRequest(String player, CompletableFuture<VnbxRelations> future) {
   }
}
