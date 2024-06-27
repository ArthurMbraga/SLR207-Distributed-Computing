package group.Metrics;

public class MyTimer {
  private long startTime = 0;
  private long elapsedTime = 0;

  public void start() {
    if (startTime != 0)
      throw new IllegalStateException("Timer is already running");

    startTime = System.nanoTime();
  }

  public void pause() {
    if (startTime == 0)
      throw new IllegalStateException("Timer is not running");

    elapsedTime += System.nanoTime() - startTime;
    startTime = 0;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }
}
