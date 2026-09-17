package dev.kafka.kafkautils.chatplus;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Wires {@link VnbxBridgeClient} to the real vnbx:bridge custom-payload channel, 26.2 only. */
public final class VnbxBridgeNetworking {
   private static final CustomPacketPayload.Type<Payload> TYPE =
         new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("vnbx", "bridge"));
   private static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC =
         CustomPacketPayload.codec(Payload::write, Payload::read);

   private VnbxBridgeNetworking() {
   }

   public static void register() {
      PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
      PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
      ClientPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) ->
            context.client().execute(() -> VnbxBridgeClient.receive(payload.data())));
   }

   public static void connected() {
      VnbxBridgeClient.reset();
      if (!ClientPlayNetworking.canSend(TYPE)) {
         return;
      }
      ClientPlayNetworking.send(new Payload(VnbxBridgeClient.request("hello")));
   }

   public static void disconnected() {
      VnbxBridgeClient.reset();
   }

   public static boolean available() {
      return ClientPlayNetworking.canSend(TYPE);
   }

   public static CompletableFuture<VnbxRelations> requestPlayerRelations(String player) {
      if (!available()) {
         return CompletableFuture.completedFuture(VnbxRelations.unavailable(player));
      }
      return VnbxBridgeClient.requestPlayerRelations(player, data -> {
         if (ClientPlayNetworking.canSend(TYPE)) {
            ClientPlayNetworking.send(new Payload(data));
         }
      });
   }

   private record Payload(byte[] data) implements CustomPacketPayload {
      private static Payload read(RegistryFriendlyByteBuf buffer) {
         int size = buffer.readableBytes();
         if (size > 16 * 1024) {
            buffer.skipBytes(size);
            return new Payload(null);
         }
         byte[] data = new byte[size];
         buffer.readBytes(data);
         return new Payload(data);
      }

      private void write(RegistryFriendlyByteBuf buffer) {
         buffer.writeBytes(data);
      }

      @Override
      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
