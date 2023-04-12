package coordinator;

import java.rmi.Remote;

import server.TwoPCServer;

/**
 * Interface for implementing a coordinator that uses Two phase commit protocol
 */
public interface TwoPCCoordinator extends Remote {

  /**
   *Method implementing the Two-phase commit protocol
   * @param operation operation to execute
   * @param key key used
   * @param value value used
   * @return 200 code if success, else 400
   */
  String twoPhaseProtocol(String operation, int key, String value);

  /**
   * Registers the server to the coordinator
   * @param server server instance to register
   */
  void registerNewServer(TwoPCServer server);
}
