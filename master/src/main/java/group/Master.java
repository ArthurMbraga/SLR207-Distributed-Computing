package group;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Master {
  // static final String[] SERVERS = { "tp-1a201-17", "tp-1a201-18", "tp-1a201-19"
  // };
  static final String[] SERVERS = { "localhost" };
  static final int FTP_PORT = 3456;
  static final int SOCKET_PORT = 2234;

  static ExecutorService executor = Executors.newFixedThreadPool(SERVERS.length);

  public static void main(String[] args) {
    FTPMultiClient ftpMultiClient = new FTPMultiClient(SERVERS, FTP_PORT);
    SocketMultiClient socketConnections = new SocketMultiClient(SERVERS, SOCKET_PORT);

    try {
      socketConnections.start();
      ftpMultiClient.start();

      /* ----------- */
      /* Split phase */
      /* ----------- */
      System.out.println("Starting split phase");

      List<String> fileLines = Utils.readInputStream(Master.class.getResourceAsStream("/sampleText.txt"));
      List<String> filesContent = Utils.groupStringList(fileLines, SERVERS.length);
      CompletableFuture<?>[] futures = new CompletableFuture[SERVERS.length];

      for (int i = 0; i < SERVERS.length; i++) {
        int serverIndex = i;
        String content = filesContent.get(i);

        futures[i] = CompletableFuture.runAsync(() -> {
          try {
            ftpMultiClient.sendFile(serverIndex, "Split.txt", content);
            System.out.println("Sent file to server " + serverIndex);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }, executor).thenRunAsync(() -> {
          try {
            socketConnections.sendMessage(serverIndex, "File transfer complete for server " + serverIndex);
            System.out.println("Sent message to server " + serverIndex);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }, executor);
      }

      // Wait for all tasks to complete
      CompletableFuture.allOf(futures).join();

    } catch (

    Exception e) {
      e.printStackTrace();
    }
  }
}
