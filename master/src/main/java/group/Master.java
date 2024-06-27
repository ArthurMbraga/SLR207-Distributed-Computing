package group;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import group.Metrics.MetricCollector;

public class Master {
  private static String[] servers;

  public static void main(String[] args) {
    servers = args[0].split(",");

    FTPMultiClient ftpMultiClient = new FTPMultiClient(servers, Constants.FTP_PORT);
    SocketMultiClient socketConnections = new SocketMultiClient(servers, Constants.SOCKET_PORT);

    try {
      socketConnections.start();
      ftpMultiClient.start();

      /* ----------- */
      /* Split phase */
      /* ----------- */
      System.out.println("Starting split phase");

      InputStream inputStream = new FileInputStream(
          "/cal/commoncrawl/CC-MAIN-20230320144934-20230320174934-00001.warc.wet");

      System.out.println("Reading lines");
      List<String> fileLines = Utils.readInputStream(inputStream);

      System.out.println("Grouping lines");
      List<String> filesContent = Utils.groupStringList(fileLines, servers.length);

      System.out.println("Sending files to servers");
      CompletableFuture<?>[] futures = new CompletableFuture[servers.length];

      MetricCollector metricCollector = new MetricCollector(servers.length);

      // Timers 
      final String COMM = "Communication";
      final String SYNC = "Synchronization";
      final String COMP = "Computation";

      final String SPLT = "Split";
      final String MAP1 = "Map1";
      final String SUF1 = "Shuffle1";
      final String RED1 = "Reduce1";

      final String MAP2 = "Map2";
      final String SUF2 = "Shuffle2";
      final String RED2 = "Reduce2";

      for (int i = 0; i < servers.length; i++) {
        int serverIndex = i;
        String content = filesContent.get(serverIndex);

        futures[serverIndex] = CompletableFuture
            .runAsync(() -> {
              try {
                metricCollector.start(serverIndex, COMM, SPLT);
                System.out.println("Sending SPLIT1 file to server " + serverIndex);

                ftpMultiClient.sendFile(serverIndex, Constants.SPLIT_FILE_NAME, content);

                System.out.println("SPLIT1 finish from server " + serverIndex);
                metricCollector.pause(serverIndex, COMM, SPLT);
              } catch (Exception e) {
                e.printStackTrace();
              }
            })
            .thenRunAsync(() -> {
              try {
                metricCollector.start(serverIndex, SYNC);
                System.out.println("Sending IPS to server " + serverIndex);

                String serverList = makeIpsMessage(serverIndex);
                socketConnections.sendMessage(serverIndex, serverList);

                System.out.println("ACK from server " + serverIndex);
                metricCollector.pause(serverIndex, SYNC);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }).thenRunAsync(() -> {
              try {
                metricCollector.start(serverIndex, COMP, MAP1);

                System.out.println("Sending 'MAP' to server " + serverIndex);

                CompletableFuture<String>[] responsesFutures = socketConnections.sendMessage(serverIndex, "MAP", 2);

                // First response: Map
                responsesFutures[0].join();

                System.out.println("MAP finish from server " + serverIndex);
                metricCollector.pause(serverIndex, COMP, MAP1);
                metricCollector.start(serverIndex, COMM, SUF1);

                // Second response: Shuffle
                responsesFutures[1].join();
                metricCollector.pause(serverIndex, COMM, SUF1);

                System.out.println("SHUFFLE finish from server " + serverIndex);
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
      futures = new CompletableFuture[servers.length];

      // range = [min, max]
      Integer[] range = { Integer.MAX_VALUE, 0 };
      for (int i = 0; i < servers.length; i++) {
        int serverIndex = i;

        futures[serverIndex] = CompletableFuture.runAsync(() -> {
          try {
            metricCollector.start(serverIndex, COMP, RED1);
            String response = socketConnections.sendMessage(serverIndex, "REDUCE");

            String[] minMax = response.split(",");

            int min = Integer.parseInt(minMax[0]);
            int max = Integer.parseInt(minMax[1]);

            if (min < range[0])
              range[0] = min;

            if (max > range[1])
              range[1] = max;

            metricCollector.pause(serverIndex, COMP, RED1);
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
      String groupMessage = makeGroupsMessage(range[0], range[1] + 1, servers.length);

      futures = new CompletableFuture[servers.length];
      for (int i = 0; i < servers.length; i++) {
        int serverIndex = i;

        futures[serverIndex] = CompletableFuture.runAsync(() -> {
          try {
            metricCollector.start(serverIndex, COMP, MAP2);

            System.out.println("Sending GROUP message to server " + serverIndex);
            CompletableFuture<String>[] responses = socketConnections.sendMessage(serverIndex, groupMessage, 2);

            // First response: MAP2
            responses[0].join();
            System.out.println("MAP2 finish from server " + serverIndex);
            metricCollector.pause(serverIndex, COMP, MAP2);
            metricCollector.start(serverIndex, COMM, SUF2);
            // Second response: SHUFFLE2
            responses[1].join();
            metricCollector.pause(serverIndex, COMM, SUF2);
            System.out.println("SHUFFLE2 finish from server " + serverIndex);
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

      futures = new CompletableFuture[servers.length];
      for (int i = 0; i < servers.length; i++) {
        int serverIndex = i;

        futures[serverIndex] = CompletableFuture.runAsync(() -> {
          try {
            metricCollector.start(serverIndex, COMP, RED2);

            socketConnections.sendMessageAsync(serverIndex, "REDUCE2");

            System.out.println("Sent REDUCE2 to server " + serverIndex);
            metricCollector.pause(serverIndex, COMP, RED2);
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

      futures = new CompletableFuture[servers.length];
      for (int i = 0; i < servers.length; i++) {
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
      ftpMultiClient.stop();

      /* --------------- */
      /* Writing Metrics */
      /* --------------- */
      Map<String, Number> settings = new HashMap<String, Number>() {
        {
          put("Number of nodes", servers.length);
        }
      };

      metricCollector.writeMetrics(Constants.METRICS_FILE, settings);
      System.out.println("Metrics saved");

      long comm = metricCollector.getLongestElapsedTime(COMM);
      long sync = metricCollector.getLongestElapsedTime(SYNC);
      long comp = metricCollector.getLongestElapsedTime(COMP);

      System.out.println("Total elapsed time: " + (comm + sync + comp));
      System.out.println("Communication: " + comm);
      System.out.println("Synchronization: " + sync);
      System.out.println("Computation: " + comp);

      double metric = (double) (comm + sync) / comp;
      System.out.println("Metric: " + metric);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static String makeIpsMessage(int targetIp) {
    String target = servers[targetIp];
    return "IPS " + target + ";" +
        IntStream.range(0, servers.length)
            .mapToObj(index -> servers[index])
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
