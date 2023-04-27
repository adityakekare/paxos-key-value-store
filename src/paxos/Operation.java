package paxos;

import java.io.Serializable;

/**
 * Class to encapsulate an operation on the Key-Value store
 */
public class Operation implements Serializable {
  private RequestMethod method;
  private int key;
  private String value;

  public Operation(RequestMethod method, int key, String value) {
    this.method = method;
    this.key = key;
    this.value = value;
  }

  public Operation(RequestMethod method, int key) {
    this.method = method;
    this.key = key;
    this.value = null;
  }

  public RequestMethod getMethod() {
    return method;
  }

  public void setMethod(RequestMethod method) {
    this.method = method;
  }

  public int getKey() {
    return key;
  }

  public void setKey(int key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
