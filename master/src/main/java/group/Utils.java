package group;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Utils {
  // Function that set each line based on the index returned
  // from the operation index % numberOfGroups
  public static List<String> groupStringList(List<String> lines, int numberOfGroups) {
    List<String> messages = new ArrayList<>();
    for (int i = 0; i < lines.size(); i++) {
      int index = i % numberOfGroups;
      if (index >= messages.size())
        messages.add(lines.get(i));
      else
        messages.set(index, messages.get(index) + lines.get(i));
    }
    return messages;
  }

  public static List<String> readInputStream(InputStream in) throws IOException {
    List<String> contentList = new ArrayList<>();

    BufferedReader reader = new BufferedReader(
        new InputStreamReader(in));
    String line;
    while ((line = reader.readLine()) != null)
      contentList.add(line);

    reader.close();
    return contentList;
  }

}
