package dev.kafka.kafkautils.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * WebSocket transport for the messenger relay (ws:// or wss://, e.g. behind
 * Cloudflare). Connects lazily, авто-reconnect with backoff, and queues inbound
 * lines so the game thread can drain them from a module tick.
 */
public final class RelayClient {
   public static final ConcurrentLinkedQueue<String> INBOX = new ConcurrentLinkedQueue<>();

   private static volatile WebSocket ws;
   private static volatile boolean connecting;
   private static volatile boolean authed;
   private static volatile long nextRetry;
   private static volatile long retryDelay = 3000L;

   private RelayClient() {
   }

   public static boolean isAuthed() {
      return authed && ws != null;
   }

   public static String status() {
      if (isAuthed()) {
         return "§a● релей";
      }
      return connecting ? "§e● подключение…" : "§c● оффлайн";
   }

   /** Call every tick: connects (with backoff) when there is no live connection. */
   public static void ensure(String url, String key, String name) {
      if (ws != null || connecting || url == null || url.isBlank() || name == null || name.isBlank()) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now < nextRetry) {
         return;
      }
      connecting = true;
      try {
         HttpClient.newHttpClient().newWebSocketBuilder()
            .buildAsync(URI.create(url.trim()), new Listener(key, name))
            .whenComplete((sock, err) -> {
               if (err != null) {
                  markDown();
               }
            });
      } catch (Exception e) {
         markDown();
      }
   }

   public static boolean send(String line) {
      WebSocket w = ws;
      if (w == null) {
         return false;
      }
      try {
         w.sendText(line, true);
         return true;
      } catch (Exception e) {
         markDown();
         return false;
      }
   }

   public static void close() {
      WebSocket w = ws;
      ws = null;
      authed = false;
      connecting = false;
      if (w != null) {
         try {
            w.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
         } catch (Exception ignored) {
         }
      }
   }

   public static String enc(String s) {
      return URLEncoder.encode(s, StandardCharsets.UTF_8);
   }

   private static void markDown() {
      ws = null;
      authed = false;
      connecting = false;
      nextRetry = System.currentTimeMillis() + retryDelay;
      retryDelay = Math.min(60000L, retryDelay * 2);
   }

   private static final class Listener implements WebSocket.Listener {
      private final String key;
      private final String name;
      private final StringBuilder buffer = new StringBuilder();

      Listener(String key, String name) {
         this.key = key;
         this.name = name;
      }

      public void onOpen(WebSocket sock) {
         ws = sock;
         connecting = false;
         retryDelay = 3000L;
         sock.sendText("auth|" + enc(this.name) + "|" + enc(this.key), true);
         sock.request(1);
      }

      public CompletionStage<?> onText(WebSocket sock, CharSequence data, boolean last) {
         this.buffer.append(data);
         if (last) {
            String line = this.buffer.toString();
            this.buffer.setLength(0);
            if (line.equals("ok")) {
               authed = true;
            }
            INBOX.add(line);
         }
         sock.request(1);
         return null;
      }

      public CompletionStage<?> onClose(WebSocket sock, int statusCode, String reason) {
         markDown();
         return null;
      }

      public void onError(WebSocket sock, Throwable error) {
         markDown();
      }
   }
}
