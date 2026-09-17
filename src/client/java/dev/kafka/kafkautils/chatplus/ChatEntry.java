package dev.kafka.kafkautils.chatplus;

/** A single captured chat line: when it arrived, its text, and which tab it belongs to. */
public record ChatEntry(long timestamp, String text, ChatTab tab) {
}
