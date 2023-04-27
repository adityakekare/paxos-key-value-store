package paxos;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Static class to generate proposals with unique IDs
 */
public final class PaxosUtils implements Serializable {

  private static int time = 0;

  public static synchronized Proposal createProposal(Operation operation){
    int proposalId = time++;

    Proposal proposal = new Proposal(proposalId, operation);

    try{
      Thread.sleep(1);
    } catch (InterruptedException e) {
      System.out.println("Server error");
    }
    return proposal;
  }
}
