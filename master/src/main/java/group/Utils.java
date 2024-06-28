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
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Utils {
  // Function that set each line based on the index returned
  // from the operation index % numberOfGroups
  public static List<String> groupStringList(List<String> lines, int numberOfGroups) {
    return IntStream.range(0, numberOfGroups).parallel().mapToObj(groupIndex -> IntStream.range(0, lines.size())
        .filter(index -> index % numberOfGroups == groupIndex)
        .mapToObj(lines::get)
        .collect(Collectors.joining("\n"))).collect(Collectors.toList());
  }

  public static List<String> readInputStream(InputStream in, float percentage) throws IOException {
    List<String> contentList = new ArrayList<>();
    Random random = new Random(12345); // Fixed seed for reproducibility

    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    String line;

    while ((line = reader.readLine()) != null) {
      if (random.nextDouble() < percentage)
        contentList.add(line);
    }
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
