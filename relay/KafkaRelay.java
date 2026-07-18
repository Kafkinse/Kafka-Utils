import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/**
 * Kafka-Utils messenger relay — a tiny standalone WebSocket server (pure JDK,
 * no dependencies). Messages travel player-to-player over this relay and never
 * touch the Minecraft server.
 *
 * Run:  java KafkaRelay.java <port> <secret>
 * e.g.  java KafkaRelay.java 8765 mySecretKey123
 *
 * Protocol: '|'-separated fields, each URL-encoded (UTF-8).
 *   client -> server: auth|name|key ; msg|to|text ; gmsg|group|text ;
 *                     gcreate|group ; gadd|group|nick
 *   server -> client: ok ; err|reason ; msg|from|text|ts ;
 *                     gmsg|group|from|text|ts ; gsys|group|text ; users|a,b ;
 *                     join|n ; left|n ; groups|g1,g2
 * Groups are persisted to groups.txt next to this file.
 */
public class KafkaRelay {
   private static final Map<String, Client> CLIENTS = new ConcurrentHashMap<>();
   private static final Map<String, Group> GROUPS = new ConcurrentHashMap<>();
   private static final Path GROUPS_FILE = Path.of("groups.txt");
   private static String secret = "";

   public static void main(String[] args) throws Exception {
      int port = args.length > 0 ? Integer.parseInt(args[0]) : 8765;
      secret = args.length > 1 ? args[1] : "";
      if (secret.isEmpty()) {
         System.err.println("Usage: java KafkaRelay.java <port> <secret>");
         return;
      }
      loadGroups();
      try (ServerSocket server = new ServerSocket(port)) {
         System.out.println("[KafkaRelay] listening on :" + port);
         while (true) {
            Socket s = server.accept();
            new Thread(() -> handle(s)).start();
         }
      }
   }

   // --- per-connection ----------------------------------------------------

   private static void handle(Socket sock) {
      Client c = new Client(sock);
      try {
         sock.setTcpNoDelay(true);
         if (!c.wsHandshake()) {
            sock.close();
            return;
         }
         String line;
         while ((line = c.readFrame()) != null) {
            onLine(c, line);
         }
      } catch (Exception ignored) {
      } finally {
         if (c.name != null && CLIENTS.remove(c.name.toLowerCase(), c)) {
            broadcast("left|" + enc(c.name), null);
         }
         try { sock.close(); } catch (IOException ignored) {}
      }
   }

   private static void onLine(Client c, String line) {
      String[] p = line.split("\\|", -1);
      String t = p[0];
      if (t.equals("auth") && p.length >= 3) {
         String name = dec(p[1]).trim();
         if (!dec(p[2]).equals(secret)) { c.send("err|" + enc("неверный ключ")); return; }
         if (name.isEmpty() || name.length() > 16) { c.send("err|" + enc("плохой ник")); return; }
         c.name = name;
         Client old = CLIENTS.put(name.toLowerCase(), c);
         if (old != null && old != c) { try { old.sock.close(); } catch (IOException ignored) {} }
         c.send("ok");
         c.send("users|" + enc(String.join(",", onlineNames())));
         c.send("groups|" + enc(String.join(",", groupsOf(name))));
         broadcast("join|" + enc(name), c);
         System.out.println("[KafkaRelay] + " + name);
         return;
      }
      if (c.name == null) { c.send("err|" + enc("нет авторизации")); return; }

      switch (t) {
         case "msg" -> {
            if (p.length < 3) return;
            String to = dec(p[1]);
            Client dst = CLIENTS.get(to.toLowerCase());
            if (dst == null) { c.send("err|" + enc(to + " не в сети (релей)")); return; }
            dst.send("msg|" + enc(c.name) + "|" + p[2] + "|" + System.currentTimeMillis());
         }
         case "gmsg" -> {
            if (p.length < 3) return;
            Group g = GROUPS.get(dec(p[1]).toLowerCase());
            if (g == null || !g.members.contains(c.name.toLowerCase())) { c.send("err|" + enc("нет такой группы")); return; }
            String out = "gmsg|" + enc(g.name) + "|" + enc(c.name) + "|" + p[2] + "|" + System.currentTimeMillis();
            for (String m : g.members) {
               Client dst = CLIENTS.get(m);
               if (dst != null && dst != c) {
                  dst.send(out);
               }
            }
         }
         case "gcreate" -> {
            if (p.length < 2) return;
            String name = dec(p[1]).trim();
            if (name.isEmpty() || name.length() > 24) { c.send("err|" + enc("плохое имя группы")); return; }
            if (GROUPS.containsKey(name.toLowerCase())) { c.send("err|" + enc("группа уже есть")); return; }
            Group g = new Group(name, c.name.toLowerCase());
            g.members.add(c.name.toLowerCase());
            GROUPS.put(name.toLowerCase(), g);
            saveGroups();
            c.send("groups|" + enc(String.join(",", groupsOf(c.name))));
            c.send("err|" + enc("группа «" + name + "» создана"));
         }
         case "gadd" -> {
            if (p.length < 3) return;
            Group g = GROUPS.get(dec(p[1]).toLowerCase());
            String nick = dec(p[2]).trim();
            if (g == null || !g.members.contains(c.name.toLowerCase())) { c.send("err|" + enc("нет такой группы")); return; }
            if (nick.isEmpty() || nick.length() > 16) return;
            if (g.members.add(nick.toLowerCase())) {
               saveGroups();
               // Tell every member of the group who added whom.
               String note = c.name + " добавил " + nick + " в группу «" + g.name + "»";
               String sys = "gsys|" + enc(g.name) + "|" + enc(note);
               for (String m : g.members) {
                  Client dst2 = CLIENTS.get(m);
                  if (dst2 != null) {
                     dst2.send(sys);
                  }
               }
               Client dst = CLIENTS.get(nick.toLowerCase());
               if (dst != null) {
                  dst.send("groups|" + enc(String.join(",", groupsOf(dst.name))));
               }
            } else {
               c.send("err|" + enc(nick + " уже в группе"));
            }
         }
         default -> { }
      }
   }

   // --- helpers -----------------------------------------------------------

   private static List<String> onlineNames() {
      List<String> out = new ArrayList<>();
      for (Client c : CLIENTS.values()) {
         out.add(c.name);
      }
      Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
      return out;
   }

   private static List<String> groupsOf(String name) {
      List<String> out = new ArrayList<>();
      for (Group g : GROUPS.values()) {
         if (g.members.contains(name.toLowerCase())) {
            out.add(g.name);
         }
      }
      Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
      return out;
   }

   private static void broadcast(String line, Client except) {
      for (Client c : CLIENTS.values()) {
         if (c != except) {
            c.send(line);
         }
      }
   }

   private static synchronized void saveGroups() {
      try {
         StringBuilder sb = new StringBuilder();
         for (Group g : GROUPS.values()) {
            sb.append(enc(g.name)).append('|').append(g.owner).append('|').append(String.join(",", g.members)).append('\n');
         }
         Files.writeString(GROUPS_FILE, sb.toString(), StandardCharsets.UTF_8);
      } catch (IOException e) {
         System.err.println("[KafkaRelay] can't save groups: " + e);
      }
   }

   private static void loadGroups() {
      try {
         if (!Files.exists(GROUPS_FILE)) return;
         for (String line : Files.readAllLines(GROUPS_FILE, StandardCharsets.UTF_8)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 3) {
               Group g = new Group(dec(p[0]), p[1]);
               for (String m : p[2].split(",")) {
                  if (!m.isBlank()) g.members.add(m);
               }
               GROUPS.put(g.name.toLowerCase(), g);
            }
         }
         System.out.println("[KafkaRelay] groups: " + GROUPS.size());
      } catch (IOException e) {
         System.err.println("[KafkaRelay] can't load groups: " + e);
      }
   }

   private static String enc(String s) {
      return URLEncoder.encode(s, StandardCharsets.UTF_8);
   }

   private static String dec(String s) {
      try { return URLDecoder.decode(s, StandardCharsets.UTF_8); } catch (Exception e) { return ""; }
   }

   private static final class Group {
      final String name;
      final String owner;
      final Set<String> members = ConcurrentHashMap.newKeySet();
      Group(String name, String owner) { this.name = name; this.owner = owner; }
   }

   // --- minimal WebSocket implementation ---------------------------------

   private static final class Client {
      final Socket sock;
      InputStream in;
      OutputStream out;
      volatile String name;

      Client(Socket sock) { this.sock = sock; }

      boolean wsHandshake() throws Exception {
         this.in = new BufferedInputStream(sock.getInputStream());
         this.out = new BufferedOutputStream(sock.getOutputStream());
         String key = null;
         StringBuilder line = new StringBuilder();
         int prev = 0;
         int empty = 0;
         long deadline = System.currentTimeMillis() + 5000;
         while (System.currentTimeMillis() < deadline) {
            int b = in.read();
            if (b < 0) return false;
            if (b == '\n') {
               String l = line.toString().trim();
               if (l.isEmpty()) { ++empty; if (prev != 0) break; }
               String low = l.toLowerCase();
               if (low.startsWith("sec-websocket-key:")) {
                  key = l.substring(l.indexOf(':') + 1).trim();
               }
               if (l.isEmpty()) break;
               line.setLength(0);
            } else if (b != '\r') {
               line.append((char) b);
            }
            prev = b;
         }
         if (key == null) return false;
         String accept = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.US_ASCII)));
         String resp = "HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: " + accept + "\r\n\r\n";
         out.write(resp.getBytes(StandardCharsets.US_ASCII));
         out.flush();
         return true;
      }

      /** Reads one text frame (handles ping/close); null on close. */
      String readFrame() throws IOException {
         while (true) {
            int b0 = in.read();
            if (b0 < 0) return null;
            int b1 = in.read();
            if (b1 < 0) return null;
            int opcode = b0 & 0x0F;
            boolean masked = (b1 & 0x80) != 0;
            long len = b1 & 0x7F;
            if (len == 126) { len = ((long) in.read() << 8) | in.read(); }
            else if (len == 127) { len = 0; for (int i = 0; i < 8; ++i) len = (len << 8) | in.read(); }
            if (len > 65536) return null;
            byte[] mask = new byte[4];
            if (masked) { readFully(mask); }
            byte[] data = new byte[(int) len];
            readFully(data);
            if (masked) { for (int i = 0; i < data.length; ++i) data[i] ^= mask[i & 3]; }
            switch (opcode) {
               case 0x1: return new String(data, StandardCharsets.UTF_8);
               case 0x8: return null;
               case 0x9: this.writeFrame(0xA, data); break; // ping -> pong
               default: break; // ignore pong/binary/continuation
            }
         }
      }

      void readFully(byte[] buf) throws IOException {
         int off = 0;
         while (off < buf.length) {
            int r = in.read(buf, off, buf.length - off);
            if (r < 0) throw new EOFException();
            off += r;
         }
      }

      void send(String text) {
         try {
            this.writeFrame(0x1, text.getBytes(StandardCharsets.UTF_8));
         } catch (IOException e) {
            try { sock.close(); } catch (IOException ignored) {}
         }
      }

      synchronized void writeFrame(int opcode, byte[] data) throws IOException {
         out.write(0x80 | opcode);
         if (data.length < 126) {
            out.write(data.length);
         } else {
            out.write(126);
            out.write((data.length >> 8) & 0xFF);
            out.write(data.length & 0xFF);
         }
         out.write(data);
         out.flush();
      }
   }
}
