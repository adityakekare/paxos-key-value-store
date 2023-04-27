package coordinator;

import java.rmi.Remote;
import java.rmi.RemoteException;

import paxos.Proposal;
import server.PaxosServer;

/**
 * Interface for implementing a coordinator that uses Paxos Algorithm
 */
public interface PaxosCoordinator extends Remote {

  /**
   * Method implementing the Paxos algorithm for commits
   * @param proposal proposal to execute
   * @return 200 code if success, else 400
   */
  String executeOperation(Proposal proposal) throws RemoteException;

  /**
   * Registers the server to the coordinator
   * @param server server instance to register
   */
  void registerNewServer(PaxosServer server);
}
