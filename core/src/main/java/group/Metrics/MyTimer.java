package group.Metrics;

public class MyTimer {
  private boolean running = false;
  private long startTime = 0;
  private long elapsedTime = 0;

  public void start() {
    if (running)
      throw new IllegalStateException("Timer is already running");

    running = true;
    startTime = System.nanoTime();
  }

  public void pause() {
    if (!running)
      throw new IllegalStateException("Timer is not running");

    running = false;
    elapsedTime += System.nanoTime() - startTime;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }
}
