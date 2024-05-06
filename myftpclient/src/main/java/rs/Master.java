package rs;

public class Master {
  static final String[] SERVERS = { "tp-1a201-17", "tp-1a201-18", "tp-1a201-19" };

  public static void main(String[] args) {

    try {
      FTPConnection.sendSplit();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
