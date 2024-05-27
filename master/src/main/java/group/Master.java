package group;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        futures[i] = CompletableFuture
            .runAsync(() -> {
              try {
                System.out.println("Sending IPS to server " + serverIndex);

                String serverList = makeIpsMessage(serverIndex);
                socketConnections.sendMessageAsync(serverIndex, serverList);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }, executor)
            .thenRunAsync(() -> {
              try {
                System.out.println("Sending SPLIT file to server " + serverIndex);
                ftpMultiClient.sendFile(serverIndex, "Split.txt", content);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }, executor).thenRunAsync(() -> {
              try {
                System.out.println("Sending 'START MAP' to server " + serverIndex);
                socketConnections.sendMessage(serverIndex, "MAP");
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

  public static String makeIpsMessage(int targetIp) {
    String target = SERVERS[targetIp];
    return "IPS " + target + ";" +
        IntStream.range(0, SERVERS.length)
            .mapToObj(index -> SERVERS[index])
            .collect(Collectors.joining(";"));
  }
}
