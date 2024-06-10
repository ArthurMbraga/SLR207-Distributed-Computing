package group;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Master {
  static final String[] SERVERS = { "tp-1a201-17", "tp-1a201-18", "tp-1a201-19" };
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

      InputStream inputStream = new FileInputStream(
          "/cal/commoncrawl/CC-MAIN-20230320144934-20230320174934-00001.warc.wet");

      List<String> fileLines = Utils.readInputStream(inputStream);
      List<String> filesContent = Utils.groupStringList(fileLines, SERVERS.length);
      CompletableFuture<?>[] futures = new CompletableFuture[SERVERS.length];

      MyMultipleTimer communication = new MyMultipleTimer(SERVERS.length);
      MyMultipleTimer synchronization = new MyMultipleTimer(SERVERS.length);
      MyMultipleTimer computation = new MyMultipleTimer(SERVERS.length);

      for (int i = 0; i < SERVERS.length; i++) {
        int serverIndex = i;
        String content = filesContent.get(i);

        futures[serverIndex] = CompletableFuture
            .runAsync(() -> {
              try {
                communication.start(serverIndex);
                System.out.println("Sending SPLIT file to server " + serverIndex);

                ftpMultiClient.sendFile(serverIndex, Constants.SPLIT_FILE_NAME, content);

                System.out.println("SPLIT finish from server " + serverIndex);
                communication.pause(serverIndex);
              } catch (Exception e) {
                e.printStackTrace();
              }
            })
            .thenRunAsync(() -> {
              try {
                synchronization.start(serverIndex);
                System.out.println("Sending IPS to server " + serverIndex);

                String serverList = makeIpsMessage(serverIndex);
                socketConnections.sendMessage(serverIndex, serverList);

                System.out.println("ACK from server " + serverIndex);
                synchronization.pause(serverIndex);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }).thenRunAsync(() -> {
              try {
                computation.start(serverIndex);
                System.out.println("Sending 'MAP' to server " + serverIndex);

                CompletableFuture<String>[] responsesFutures = socketConnections.sendMessage(serverIndex, "MAP", 2);

                // First response: Map
                responsesFutures[0].join();
                computation.pause(serverIndex);

                // Second response: Shuffle
                communication.start(serverIndex);
                responsesFutures[1].join();
                communication.pause(serverIndex);

                System.out.println("MAP finish from server " + serverIndex);
                computation.pause(serverIndex);
              } catch (Exception e) {
                e.printStackTrace();
              }
            });
      }

      CompletableFuture.allOf(futures).join();

      /* ------------ */
      /* REDUCE phase */
      /* ------------ */
      System.out.println("Starting reduce phase");
      futures = new CompletableFuture[SERVERS.length];

      // range = [min, max]
      Integer[] range = { Integer.MAX_VALUE, 0 };
      for (int i = 0; i < SERVERS.length; i++) {
        int serverIndex = i;

        futures[serverIndex] = CompletableFuture.runAsync(() -> {
          try {
            computation.start(serverIndex);
            String response = socketConnections.sendMessage(serverIndex, "REDUCE");

            String[] minMax = response.split(",");

            int min = Integer.parseInt(minMax[0]);
            int max = Integer.parseInt(minMax[1]);

            if (min < range[0])
              range[0] = min;

            if (max > range[1])
              range[1] = max;

            computation.pause(serverIndex);
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
            computation.start(serverIndex);

            System.out.println("Sending GROUP message to server " + serverIndex);
            CompletableFuture<String>[] responses = socketConnections.sendMessage(serverIndex, groupMessage, 2);

            // First response: MAP2
            responses[0].join();
            computation.pause(serverIndex);

            // Second response: SHUFFLE2
            communication.start(serverIndex);
            responses[1].join();
            communication.pause(serverIndex);

            synchronization.pause(serverIndex);
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
            computation.start(serverIndex);
            socketConnections.sendMessageAsync(serverIndex, "REDUCE2");
            System.out.println("Sent REDUCE2 to server " + serverIndex);
            computation.pause(serverIndex);

          } catch (Exception e) {
            e.printStackTrace();
          }
        });
      }

      CompletableFuture.allOf(futures).join();

      /* -------- */
      /* Finished */
      /* -------- */
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

      /* --------------- */
      /* Writing Metrics */
      /* --------------- */
      long comm = communication.getLongestElapsedTime();
      long sync = synchronization.getLongestElapsedTime();
      long comp = computation.getLongestElapsedTime();

      System.out.println("Total elapsed time: " + comm + sync + comp);
      System.out.println("Communication: " + comm);
      System.out.println("Synchronization: " + sync);
      System.out.println("Computation: " + comp);

      long metric = (comm + sync) / comp;
      System.out.println("Metric: " + metric);

    } catch (Exception e) {
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
