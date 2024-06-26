package group;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
          if (j % 200000 == 0)
            System.out.println("Line " + j + " out of " + lines.size());

          sb.append(lines.get(j)).append("\n");
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

  public static void saveMetrics(int nNodes, long comm, long sync, long comp, double metric) {
      try {
        String fileName = "results.csv";
  
        File file = new File(fileName);
        boolean fileExists = file.exists();
  
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
        if (!fileExists) 
          writer.write("Total elapsed time,Number of nodes,Communication,Synchronization,Computation,Metric\n");
        
        writer.write((comm + sync + comp) + "," + nNodes + "," + comm + "," + sync + "," + comp + "," + metric + "\n");
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
  
    }
}
