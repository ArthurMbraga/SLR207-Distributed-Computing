package group;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Utils {
  // Function that set each line based on the index returned
  // from the operation index % numberOfGroups
  public static List<String> groupStringList(List<String> lines, int numberOfGroups) {
    List<String> messages = new ArrayList<>(numberOfGroups);
    List<Thread> threads = new ArrayList<>();

    for (int i = 0; i < numberOfGroups; i++) {
      messages.add("");
      final int index = i;
      StringBuilder sb = new StringBuilder();

      Thread thread = new Thread(() -> {
        for (int j = index; j < lines.size(); j += numberOfGroups) {
          if (j % 1000 == 0)
            System.out.println("Line " + j + " out of " + lines.size());

          sb.append(" ").append(lines.get(j));
        }
        messages.set(index, sb.toString());
      });

      threads.add(thread);
      thread.start();
    }

    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    return messages;
  }

  public static List<String> readInputStream(InputStream in) throws IOException {
    List<String> contentList = new ArrayList<>();

    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    String line;
    while ((line = reader.readLine()) != null)
      contentList.add(line);

    reader.close();
    return contentList;
  }

  public static void writeMetricsToFile(String fileName, long comm, long sync, long comp, double metric) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
      writer.write("Total elapsed time: " + (comm + sync + comp) + "\n");
      writer.write("Communication: " + comm + "\n");
      writer.write("Synchronization: " + sync + "\n");
      writer.write("Computation: " + comp + "\n");
      writer.write("Metric: " + metric + "\n");
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
