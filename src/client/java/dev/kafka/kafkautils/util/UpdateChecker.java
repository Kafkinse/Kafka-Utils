package dev.kafka.kafkautils.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Checks GitHub Releases once per session for a newer Kafka-Utils version.
 * Pure Java (HTTP client, regex, Fabric Loader's own mapping-agnostic API) —
 * no Minecraft type is touched, so this is shared by both targets. Each
 * target's tick loop polls {@link #consumeAvailable()} once and shows it
 * however fits that target (a local chat line, a toast, etc.) since actually
 * displaying something needs a real Minecraft API call.
 */
public final class UpdateChecker {
   private static final String MOD_ID = "kafkautils";
   private static final String RELEASES_API = "https://api.github.com/repos/Kafkinse/Kafka-Utils/releases/latest";
   private static final String RELEASES_PAGE = "https://github.com/Kafkinse/Kafka-Utils/releases/latest";
   private static final int MAX_BODY_LENGTH = 65_536;
   private static final Pattern TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([0-9]+(?:\\.[0-9]+){0,2})\"");
   private static final HttpClient CLIENT = HttpClient.newBuilder()
         .connectTimeout(Duration.ofSeconds(4))
         .followRedirects(HttpClient.Redirect.NEVER)
         .build();

   private static boolean started;
   private static final AtomicReference<String> available = new AtomicReference<>();

   private UpdateChecker() {
   }

   /** Starts one background check. Safe to call from every target; runs at most once per session. */
   public static void start() {
      if (started) {
         return;
      }
      started = true;
      String currentVersion = currentVersion();
      HttpRequest request = HttpRequest.newBuilder(URI.create(RELEASES_API))
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", "Kafka-Utils Update Checker")
            .header("Accept", "application/vnd.github+json")
            .GET().build();
      CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenAccept(response -> handle(response, currentVersion))
            .exceptionally(error -> null);
   }

   private static void handle(HttpResponse<String> response, String currentVersion) {
      if (response.statusCode() != 200) {
         return; // covers "no releases yet" (404) — silently do nothing
      }
      String body = response.body();
      if (body == null || body.length() > MAX_BODY_LENGTH) {
         return;
      }
      Matcher matcher = TAG_NAME.matcher(body);
      if (!matcher.find()) {
         return;
      }
      String latest = matcher.group(1);
      if (compare(latest, currentVersion) > 0) {
         available.set("§d[Kafka-Utils] §7Доступна новая версия §f" + latest
               + " §7(у тебя §f" + currentVersion + "§7): §b" + RELEASES_PAGE);
      }
   }

   /** Returns the update message once, then clears it so it's only shown a single time. */
   public static Optional<String> consumeAvailable() {
      return Optional.ofNullable(available.getAndSet(null));
   }

   private static String currentVersion() {
      return FabricLoader.getInstance().getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("0.0.0");
   }

   private static int compare(String a, String b) {
      String[] partsA = a.split("\\.");
      String[] partsB = b.split("\\.");
      for (int i = 0; i < 3; ++i) {
         int diff = part(partsA, i) - part(partsB, i);
         if (diff != 0) {
            return diff;
         }
      }
      return 0;
   }

   private static int part(String[] parts, int index) {
      if (index >= parts.length) {
         return 0;
      }
      try {
         return Integer.parseInt(parts[index].replaceAll("[^0-9]", ""));
      } catch (NumberFormatException e) {
         return 0;
      }
   }
}
