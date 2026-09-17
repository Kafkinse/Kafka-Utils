package dev.kafka.kafkautils.chatplus;

/** Clan/marriage info for one player, as reported by the server's vnbx:bridge plugin channel. */
public record VnbxRelations(
      boolean available,
      String player,
      boolean inClan,
      String clanTag,
      String clanName,
      String clanRank,
      boolean married,
      String partnerName) {

   static VnbxRelations unavailable(String player) {
      return new VnbxRelations(false, player, false, null, null, null, false, null);
   }
}
