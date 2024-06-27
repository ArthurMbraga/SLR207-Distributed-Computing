package group.Metrics;

public class MyMultipleTimer {
  private MyTimer[] timers;

  public MyMultipleTimer(int numberOfTimes) {
    timers = new MyTimer[numberOfTimes];
    for (int i = 0; i < numberOfTimes; i++) {
      timers[i] = new MyTimer();
    }
  }

  public void start(int index) {
    timers[index].start();
  }

  public void pause(int index) {
    timers[index].pause();
  }

  public long getLongestElapsedTime() {
    long longestElapsedTime = 0;
    for (MyTimer timer : timers) {
      if (timer.getElapsedTime() > longestElapsedTime) {
        longestElapsedTime = timer.getElapsedTime();
      }
    }
    return longestElapsedTime;
  }
}
