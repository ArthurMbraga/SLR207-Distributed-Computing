package group.socket;

import java.io.Serializable;

public class RefMessage implements Serializable {
  private String[] references;

  public RefMessage(String[] references) {
    this.references = references;
  }

  public String[] getReferences() {
    return references;
  }
}
