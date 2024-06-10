package group;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Server {
  static final int FTP_PORT = 3456;

  static FTPServer ftpServer;
  static FTPMultiClient ftpMultiClient;

  static SocketServer socketServer;

  static String[] servers;
  static String identifier;

  private static HashMap<String, Integer> reduceMap;

  public static void main(String[] args) {
    Logger.configure();
    ftpServer = new FTPServer();

    socketServer = new SocketServer();

    MessageHandler messageHandler = new MessageHandler();

    messageHandler.startsWith("MAP",
        (message, os) -> {
          try {
            HashMap<String, Integer> map = mapFunction();

            os.write("MAP");
            os.newLine();
            os.flush();

            List<String> shuffle = shuffleFunction(map);

            CompletableFuture<?>[] futures = new CompletableFuture[servers.length];

            IntStream.range(0, servers.length).forEach(i -> {
              futures[i] = CompletableFuture.runAsync(() -> {
                try {
                  ftpMultiClient.sendFile(i, Constants.SHUFFLE_FILE_PREFIX + "-" + identifier + ".txt", shuffle.get(i));
                } catch (IOException e) {
                  e.printStackTrace();
                }
              });
            });

            // Wait for all tasks to complete
            CompletableFuture.allOf(futures).join();
            os.write("SHUFFLE");
            os.newLine();
            os.flush();
          } catch (IOException e) {
            e.printStackTrace();
          }
        });

    messageHandler.startsWith("IPS",
        (message, os) -> {
          String[] ips = message.split(";");
          identifier = ips[0];
          servers = Arrays.copyOfRange(ips, 1, ips.length);

          try {
            ftpMultiClient = new FTPMultiClient(servers, FTP_PORT);
            ftpMultiClient.start();

            os.write("IPS");
            os.newLine();
            os.flush();
          } catch (IOException e) {
            e.printStackTrace();
          }
        });

    messageHandler.startsWith("REDUCE2",
        (message, os) -> {
          try {
            File directory = new File("/dev/shm/braga-23/");
            File[] files = directory.listFiles((dir, name) -> name.startsWith(Constants.GROUP_FILE_PREFIX));

            if (files != null) {
              HashMap<String, Integer> finalResult = reduceFunction(files);
              String fileContent = mapToString(finalResult);

              // Write file in local directory
              File file = new File("/dev/shm/braga-23/finalResult.txt");
              BufferedWriter writer = new BufferedWriter(new FileWriter(file));
              writer.write(fileContent);
              writer.close();
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        });

    messageHandler.startsWith("REDUCE",
        (message, os) -> {
          try {
            File directory = new File("/dev/shm/braga-23/");
            File[] files = directory.listFiles((dir, name) -> name.startsWith(Constants.SHUFFLE_FILE_PREFIX));

            if (files != null) {
              reduceMap = reduceFunction(files);
              Integer[] minMax = findMinMaxFreq(reduceMap);

              os.write(minMax[0] + "," + minMax[1]);
              os.newLine();
              os.flush();
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        });

    messageHandler.startsWith("GROUP",
        (message, os) -> {

          /* --- */
          /* Map */
          /* --- */

          Integer[][] ranges = Arrays.stream(message.split(";"))
              .map(r -> r.split(","))
              .map(range -> new Integer[] { Integer.parseInt(range[0]), Integer.parseInt(range[1]) })
              .toArray(Integer[][]::new);

          String[] filesContents = new String[servers.length];

          // Print ranges
          for (int i = 0; i < ranges.length; i++) {
            System.out.println("Range " + i + ": " + ranges[i][0] + " - " + ranges[i][1]);
          }

          for (Map.Entry<String, Integer> entry : reduceMap.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();

            // Find server index by searching in ranges and finding when the value is in
            // range [min, max).
            int serverIndex = IntStream.range(0, ranges.length)
                .filter(i -> value >= ranges[i][0] && value < ranges[i][1])
                .findFirst()
                .orElse(-1);

            if (filesContents[serverIndex] == null)
              filesContents[serverIndex] = key + "," + value;
            else
              filesContents[serverIndex] += "\n" + key + "," + value;
          }

          try {
            os.write("MAP2");
            os.newLine();
            os.flush();
          } catch (IOException e) {
            e.printStackTrace();
          }

          /* --------- */
          /* Shuffle 2 */
          /* --------- */

          CompletableFuture<?>[] futures = new CompletableFuture[servers.length];

          IntStream.range(0, servers.length).forEach(i -> {
            futures[i] = CompletableFuture.runAsync(() -> {
              try {
                ftpMultiClient.sendFile(i, Constants.GROUP_FILE_PREFIX + "-" + identifier + ".txt", filesContents[i]);
              } catch (IOException e) {
                e.printStackTrace();
              }
            });
          });

          // Wait for all tasks to complete
          CompletableFuture.allOf(futures).join();

          try {
            os.write("SHUFFLE2");
            os.newLine();
            os.flush();
          } catch (IOException e) {
            e.printStackTrace();
          }

        });

    messageHandler.startsWith("FINISH",
        (message, os) -> {
          try {
            os.write("FINISH");
            os.newLine();
            os.flush();

            // Clear files
            File directory = new File("/dev/shm/braga-23/");
            File[] files = directory.listFiles((dir, name) -> name.startsWith(Constants.SHUFFLE_FILE_PREFIX)
                || name.startsWith(Constants.GROUP_FILE_PREFIX));

            if (files != null)
              for (File file : files)
                file.delete();

            ftpMultiClient.close();
            socketServer.stop();
          } catch (IOException e) {
            e.printStackTrace();
          }
        });

    messageHandler.startsWith(null,
        (message, os) -> {
          System.out.println("Unhandled message: " + message);
        });

    /* ------------------- */
    /* Start socket server */
    /* ------------------- */
    socketServer.setOnReceiveMessageListener(
        (message, os) -> {
          System.out.println("Received message: " + message);
          messageHandler.handleMessage(message, os);
        });

    socketServer.start();
  }

  private static String removeSpecialCharacters(String str) {
    return str.replaceAll("[^a-zA-Z0-9 ]", "");
  }

  private static HashMap<String, Integer> mapFunction() throws FileNotFoundException, IOException {
    String line;
    HashMap<String, Integer> hashMap = new HashMap<>();
    BufferedReader reader = new BufferedReader(new FileReader("/dev/shm/braga-23/" + Constants.SPLIT_FILE_NAME));

    while ((line = reader.readLine()) != null) {
      String[] words = removeSpecialCharacters(line).toLowerCase().split(" ");
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

  private static List<String> shuffleFunction(HashMap<String, Integer> map) {
    List<List<String>> shuffle = new ArrayList<>(servers.length);

    for (int i = 0; i < servers.length; i++)
      shuffle.add(i, new ArrayList<>());

    for (String key : map.keySet()) {
      int serverIndex = Math.abs(key.hashCode()) % servers.length;

      String data = key + "," + map.get(key);
      List<String> list = shuffle.get(serverIndex);
      list.add(data);
    }

    List<String> result = new ArrayList<>();
    for (List<String> list : shuffle) {
      result.add(String.join("\n", list));
    }
    return result;
  }

  private static HashMap<String, Integer> reduceFunction(File[] files) {
    return Arrays.stream(files)
        .filter(File::isFile)
        .flatMap(file -> {
          try {
            return Files.lines(file.toPath());
          } catch (IOException e) {
            return Stream.empty();
          }
        }).map(line -> line.split(","))
        .collect(Collectors.toMap(data -> data[0], data -> Integer.parseInt(data[1]), Integer::sum, HashMap::new));
  }

  private static Integer[] findMinMaxFreq(HashMap<String, Integer> map) {
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;

    for (int value : map.values()) {
      if (value < min)
        min = value;

      if (value > max)
        max = value;

    }

    return new Integer[] { min, max };
  }

  private static String mapToString(Map<String, Integer> map) {
    return map.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> entry.getKey() + "," + entry.getValue())
        .collect(Collectors.joining("\n"));
  }
}
