package group;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Master {
  static final String[] SERVERS = { "tp-1a201-17", "tp-1a201-18", "tp-1a201-19" };
  // static final String[] SERVERS = { "localhost" };
  static final int FTP_PORT = 3456;
  static final int SOCKET_PORT = 2234;

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

        futures[serverIndex] = CompletableFuture
            .runAsync(() -> {
              try {
                System.out.println("Sending SPLIT file to server " + serverIndex);

                ftpMultiClient.sendFile(serverIndex, Constants.SPLIT_FILE_NAME, content);

                System.out.println("SPLIT finish from server " + serverIndex);
              } catch (Exception e) {
                e.printStackTrace();
              }
            })
            .thenRunAsync(() -> {
              try {
                System.out.println("Sending IPS to server " + serverIndex);

                String serverList = makeIpsMessage(serverIndex);
                socketConnections.sendMessage(serverIndex, serverList);

                System.out.println("ACK from server " + serverIndex);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }).thenRunAsync(() -> {
              try {
                System.out.println("Sending 'MAP' to server " + serverIndex);

                socketConnections.sendMessage(serverIndex, "MAP");

                System.out.println("MAP finish from server " + serverIndex);
              } catch (Exception e) {
                e.printStackTrace();
              }
            });
      }

      // Wait for all tasks to complete
      CompletableFuture.allOf(futures).join();

      /* ------------ */
      /* REDUCE phase */
      /* ------------ */
      System.out.println("Starting reduce phase");

      futures = new CompletableFuture[SERVERS.length];

      Integer[] range = { Integer.MAX_VALUE, 0 }; // [min, max]
      for (int i = 0; i < SERVERS.length; i++) {
        int serverIndex = i;

        futures[serverIndex] = CompletableFuture.runAsync(() -> {
          try {
            String response = socketConnections.sendMessage(serverIndex, "REDUCE");
            String[] minMax = response.split(",");

            int min = Integer.parseInt(minMax[0]);
            int max = Integer.parseInt(minMax[1]);

            if (min < range[0])
              range[0] = min;

            if (max > range[1])
              range[1] = max;

          } catch (Exception e) {
            e.printStackTrace();
          }
        });
      }
      CompletableFuture.allOf(futures).join();

      /* ----------- */
      /* GROUP phase */
      /* ----------- */
      System.out.println("Starting group phase");
      System.out.println("Min: " + range[0] + " Max: " + range[1]);
      String groupMessage = makeGroupsMessage(range[0], range[1] + 1, SERVERS.length);

      futures = new CompletableFuture[SERVERS.length];
      for (int i = 0; i < SERVERS.length; i++) {
        int serverIndex = i;

        futures[serverIndex] = CompletableFuture.runAsync(() -> {
          try {
            System.out.println("Sending GROUP message to server " + serverIndex);
            socketConnections.sendMessage(serverIndex, groupMessage);
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
      }
      CompletableFuture.allOf(futures).join();

      /* ------------- */
      /* REDUCE phase2 */
      /* ------------- */
      System.out.println("Starting reduce phase 2");

      futures = new CompletableFuture[SERVERS.length];
      for (int i = 0; i < SERVERS.length; i++) {
        int serverIndex = i;

        futures[serverIndex] = CompletableFuture.runAsync(() -> {
          try {
            socketConnections.sendMessageAsync(serverIndex, "REDUCE2");
            System.out.println("Sent REDUCE2 to server " + serverIndex);
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
      }
      CompletableFuture.allOf(futures).join();

      /* ------------- */
      /* Finished */
      /* ------------- */
      System.out.println("Finished");

      futures = new CompletableFuture[SERVERS.length];
      for (int i = 0; i < SERVERS.length; i++) {
        int serverIndex = i;

        futures[serverIndex] = CompletableFuture.runAsync(() -> {
          try {
            socketConnections.sendMessage(serverIndex, "FINISH");
            System.out.println("Sent FINISH to server " + serverIndex);
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
      }

      CompletableFuture.allOf(futures).join();

      socketConnections.close();
      ftpMultiClient.close();

    } catch (

    Exception e) {
      e.printStackTrace();
    }
  }

  private static String makeIpsMessage(int targetIp) {
    String target = SERVERS[targetIp];
    return "IPS " + target + ";" +
        IntStream.range(0, SERVERS.length)
            .mapToObj(index -> SERVERS[index])
            .collect(Collectors.joining(";"));
  }

  private static String makeGroupsMessage(int min, int max, int numServers) {
    int range = max - min;
    int groupSize = range / numServers;
    int remainder = range % numServers;

    return IntStream.range(0, numServers)
        .mapToObj(i -> {
          int groupMin = min + i * groupSize;
          int groupMax = groupMin + groupSize;

          if (i == numServers - 1) {
            groupMax += remainder;
          }

          return groupMin + "," + groupMax;
        })
        .collect(Collectors.joining(";", "GROUP ", ""));
  }
}
