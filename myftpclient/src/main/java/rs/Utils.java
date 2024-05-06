package rs;

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

}
