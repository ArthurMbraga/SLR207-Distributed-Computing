package group;
public class MyTimer {
  private long startTime;
  private long elapsedTime;

  public void start() {
    startTime = System.nanoTime();
  }

  public void pause() {
    elapsedTime += System.nanoTime() - startTime;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }
}
