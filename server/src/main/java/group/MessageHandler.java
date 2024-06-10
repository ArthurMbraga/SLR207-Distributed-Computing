package group;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;

public class MessageHandler {
  Map<String, MessageProcessor> processors = new HashMap<>();

  @FunctionalInterface
  interface MessageProcessor {
    void process(String message, BufferedWriter os);
  }

  public void handleMessage(String message, BufferedWriter os) {
    String[] parts = message.split(" ", 2);
    String prefix = parts[0];
    String content = parts.length > 1 ? parts[1] : "";

    processors.getOrDefault(prefix, processors.get(null)).process(content, os);
  }

  public void startsWith(String prefix, MessageProcessor messageProcessor) {
    processors.put(prefix, messageProcessor);
  }

}