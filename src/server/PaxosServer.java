package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

import paxos.Promise;
import paxos.Proposal;
import service.RpcService;

/**
 * An interface for the methods to implement a server that uses Paxos protocol
 */
public interface PaxosServer extends RpcService, Remote {
  /**
   * Method for the Prepare phase of Paxos
   * @param proposal Proposal from a proposer
   * @return a Promise including the status
   * @throws RemoteException if procedure can't be called
   */
  Promise prepare(Proposal proposal) throws RemoteException;

  /**
   * Method for the Accept phase of Paxos
   * @param proposal Proposal from a proposer
   * @return a Promise including the status
   * @throws RemoteException if procedure can't be called
   */
  Boolean accept(Proposal proposal) throws RemoteException;

  /**
   * Method for the implementing the Learner.
   * @param proposal Proposal from a proposer
   * @return a Promise including the status
   * @throws RemoteException if procedure can't be called
   */
  String learn(Proposal proposal) throws RemoteException;

  /**
   * Method for getting the port of the acceptor
   * @return integer port number
   * @throws RemoteException if procedure can't be called
   */
  int getPort() throws RemoteException;
}
