package group;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {
  static final int FTP_PORT = 3456;

  static FTPServer ftpServer;
  static FTPMultiClient ftpMultiClient;

  static SocketServer socketServer;

  static String[] servers;
  static String identifier;

  public static void main(String[] args) {
    Logger.configure();
    ftpServer = new FTPServer();

    socketServer = new SocketServer();

    MessageHandler messageHandler = new MessageHandler();

    messageHandler.startsWith("MAP", message -> {
      try {
        HashMap<String, Integer> map = mapFunction();
        System.out.println("MAP: " + map);

        HashMap<Integer, String> shuffle = shuffleFunction(map);
        System.out.println("SHUFFLE: " + shuffle);

        //TODO: parallelize this
        for (int i = 0; i < servers.length; i++) {
          ftpMultiClient.sendFile(i, "Shuffle-" + identifier + ".txt", shuffle.get(i));
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    messageHandler.startsWith("IPS",
        (message) -> {
          String[] ips = message.split(";");
          identifier = ips[0];
          servers = Arrays.copyOfRange(ips, 1, ips.length);

          System.out.println("Received IPS: " + servers);
          ftpMultiClient = new FTPMultiClient(servers, FTP_PORT);
        });

    socketServer.setOnReceiveMessageListener((message, os) -> {
      messageHandler.handleMessage(message);
    });

    socketServer.start();

  }

  private static HashMap<String, Integer> mapFunction() throws FileNotFoundException, IOException {
    String line;
    HashMap<String, Integer> hashMap = new HashMap<>();
    BufferedReader reader = new BufferedReader(new FileReader("/dev/shm/braga-23/Split.txt"));

    while ((line = reader.readLine()) != null) {
      String[] words = line.split(" ");
      for (String word : words) {
        if (hashMap.containsKey(word)) {
          hashMap.put(word, hashMap.get(word) + 1);
        } else {
          hashMap.put(word, 1);
        }
      }
    }
    reader.close();
    return hashMap;
  }

  private static HashMap<Integer, String> shuffleFunction(HashMap<String, Integer> map) {
    HashMap<Integer, List<String>> shuffle = new HashMap<>();

    for (int i = 0; i < servers.length; i++)
      shuffle.put(i, Arrays.asList());

    for (String key : map.keySet()) {
      int serverIndex = Math.abs(key.hashCode()) % servers.length;

      String data = key + "," + map.get(key);
      List<String> list = shuffle.get(serverIndex);
      list.add(data);
      shuffle.put(serverIndex, list);
    }

    HashMap<Integer, String> result = new HashMap<>();

    for (int i = 0; i < servers.length; i++) {
      String fileData = String.join("\n", shuffle.get(i));
      result.put(i, fileData);
    }

    return result;
  }
}

class MessageHandler {
  Map<String, MessageProcessor> processors = new HashMap<>();

  @FunctionalInterface
  interface MessageProcessor {
    void process(String message);
  }

  public void handleMessage(String message) {
    String[] parts = message.split(" ", 2);
    String prefix = parts[0];
    String content = parts.length > 1 ? parts[1] : "";

    processors.getOrDefault(prefix, processors.get(null)).process(content);
  }

  public void startsWith(String prefix, MessageProcessor messageProcessor) {
    processors.put(prefix, messageProcessor);
  }

}