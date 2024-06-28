package group;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Server {

  static FTPServer ftpServer;
  static FTPMultiClient ftpMultiClient;

  static SocketServer socketServer;

  static String[] servers;
  static String identifier;

  private static HashMap<String, Integer> reduceMap;

  public static void main(String[] args) {
    clearDirectory();

    Logger.configure();

    try {
      PrintStream fileOut = new PrintStream(new FileOutputStream("logs.txt"));
      System.setOut(fileOut);
      System.setErr(fileOut);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

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
          System.out.println("Received IPS: " + message);
          String[] ips = message.split(";");
          identifier = ips[0];
          servers = Arrays.copyOfRange(ips, 1, ips.length);

          try {
            System.out.println("Starting FTP multiclient server");
            ftpMultiClient = new FTPMultiClient(servers, Constants.FTP_PORT);

            os.write("IPS");
            os.newLine();
            os.flush();

            System.out.println("Sending SPLIT to server " + identifier);
          } catch (IOException e) {
            System.out.println("Error starting FTP multiclient server");
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
              File file = new File("/dev/shm/braga-23/" + Constants.FINAL_RESULT_FILE);
              BufferedWriter writer = new BufferedWriter(new FileWriter(file));
              writer.write(fileContent);
              writer.close();
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        });

    messageHandler.startsWith("REDUCE1",
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
            } else {
              System.out.println("No files found in directory");
              os.write("-1,0");
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

          // Print ranges
          for (int i = 0; i < ranges.length; i++)
            System.out.println("Range " + i + ": " + ranges[i][0] + " - " + ranges[i][1]);

          String[] filesContents = generateGroupsFiles(ranges);

          System.out.println("Generated files contents");

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
            clearDirectory();

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

  private static void clearDirectory() {
    File directory = new File("/dev/shm/braga-23/");
    File[] files = directory.listFiles((dir, name) -> name.startsWith(Constants.SHUFFLE_FILE_PREFIX)
        || name.startsWith(Constants.GROUP_FILE_PREFIX) || name.startsWith(Constants.SPLIT_FILE_NAME)
        || name.startsWith(Constants.FINAL_RESULT_FILE));

    if (files != null)
      for (File file : files)
        file.delete();
  }

  private static final Pattern SPECIAL_CHARACTERS_PATTERN = Pattern.compile("[\\p{Punct}\\p{IsPunctuation}]");

  private static String removeSpecialCharacters(String str) {
    return SPECIAL_CHARACTERS_PATTERN.matcher(str).replaceAll("");
  }

  private static HashMap<String, Integer> mapFunction() throws FileNotFoundException, IOException {
    String line;
    HashMap<String, Integer> hashMap = new HashMap<>();

    try (BufferedReader reader = new BufferedReader(new FileReader("/dev/shm/braga-23/" + Constants.SPLIT_FILE_NAME))) {
      while ((line = reader.readLine()) != null) {
        String[] words = removeSpecialCharacters(line).toLowerCase().split("\\s+");
        for (String word : words)
          hashMap.merge(word, 1, Integer::sum);
      }
    }

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
        .sorted(Map.Entry.<String, Integer>comparingByValue()
            .thenComparing(Map.Entry.comparingByKey()))
        .map(entry -> entry.getKey() + "," + entry.getValue())
        .collect(Collectors.joining("\n"));
  }

  private static String[] generateGroupsFiles(Integer[][] ranges) {
    String[] filesContents = new String[ranges.length];
    ExecutorService executor = Executors.newFixedThreadPool(ranges.length);
    CountDownLatch latch = new CountDownLatch(ranges.length);

    for (int i = 0; i < ranges.length; i++) {
      final int index = i;
      Integer[] range = ranges[index];

      executor.submit(() -> {
        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (Map.Entry<String, Integer> entry : reduceMap.entrySet()) {
          if (count % 10000 == 0 && index == ranges.length - 1) {
            System.out.println("Key " + count + " out of " + reduceMap.size());
          }

          count++;

          String key = entry.getKey();
          Integer value = entry.getValue();

          // range [min, max).
          if (value >= range[0] && value < range[1]) {
            sb.append(key).append(",").append(value).append("\n");
          }
        }

        filesContents[index] = sb.toString();
        latch.countDown();
      });
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      executor.shutdown();
    }

    reduceMap = null;

    return filesContents;
  }
}