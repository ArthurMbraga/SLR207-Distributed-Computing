package group.Metrics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricCollector {
  private int numberOfInstances;
  private Map<String, MyMultipleTimer> timerMap = new ConcurrentHashMap<>();

  public MetricCollector(int numberOfInstances) {
    this.numberOfInstances = numberOfInstances;
  }

  public void createTimer(String... name) {
    for (String timerName : name)
      timerMap.put(timerName, new MyMultipleTimer(numberOfInstances));
  }

  private void start(int instanceIndex, String timerName) {
    try {
      if (!timerMap.containsKey(timerName))
        throw new IllegalArgumentException("Timer " + timerName + " does not exist");

      timerMap.get(timerName).start(instanceIndex);
    } catch (Exception e) {
      System.out.println("Timer " + timerName + " failed to start");
      e.printStackTrace();
    }
  }

  public void start(int instanceIndex, String... timerNames) {
    for (String timerName : timerNames)
      start(instanceIndex, timerName);
  }

  private void pause(int instanceIndex, String timerName) {
    try {
      timerMap.get(timerName).pause(instanceIndex);
    } catch (Exception e) {
      System.out.println("Timer " + timerName + " failed to pause");
      e.printStackTrace();
    }
  }

  public void pause(int instanceIndex, String... timerNames) {
    for (String timerName : timerNames)
      pause(instanceIndex, timerName);
  }

  public long getLongestElapsedTime(String timerName) {
    return timerMap.get(timerName).getLongestElapsedTime();
  }

  public void writeMetrics(String fileName, Map<String, Number> configMap) {
    try {
      File file = new File(fileName);
      boolean fileExists = file.exists();

      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));

      if (!fileExists) {
        // Write the header of the file
        List<String> headers = new ArrayList<>();
        headers.addAll(timerMap.keySet());
        headers.addAll(configMap.keySet());

        String header = String.join(",", headers);
        writer.write(header + "\n");
      }

      // Write the metrics
      List<String> metricsList = new ArrayList<>();

      for (MyMultipleTimer timer : timerMap.values())
        metricsList.add(String.valueOf(timer.getLongestElapsedTime()));
      for (Number data : configMap.values())
        metricsList.add(String.valueOf(data));

      String metrics = String.join(", ", metricsList);
      writer.write(metrics + "\n");
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
