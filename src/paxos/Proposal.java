package paxos;

import java.io.Serializable;

/**
 * Class to encapsulate an Proposal in the Paxos algorithm
 */
public class Proposal implements Serializable {
  private int id;
  private Operation operation;

  public Proposal(int id, Operation operation) {
    this.id = id;
    this.operation = operation;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Operation getOperation() {
    return operation;
  }

  public void setOperation(Operation operation) {
    this.operation = operation;
  }

}
