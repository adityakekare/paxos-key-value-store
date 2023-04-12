package server;

import java.util.UUID;

/**
 * Interface for implementing a server with Two Phase Protocol for commits
 */
public interface TwoPCServer {
  /**
   * Method for Prepare phase of 2PC
   * @param operation Operation type
   * @param key key used
   * @param cid client ID making the operation
   * @return True or false based on success of prepare phase
   */
  boolean confirmPrepare(String operation, int key, UUID cid);

  /**
   * Method for commit phase of 2PC
   * @param operation Operation type
   * @param key key used
   * @param value value used
   * @param cid client ID making the operation
   * @return True or false based on the success of commit phase
   */
  boolean confirmCommit(String operation, int key, String value, UUID cid);

  /**
   * Method for executing the GET request on the server
   * @param key key on which operation is executed
   * @return value for the key
   */
  String executeGet(int key);

  /**
   * Method for executing the PUT request on the server
   * @param key key on which operation is executed
   */
  void executePut(int key, String value);

  /**
   * Method for executing the DELETE request on the server
   * @param key key on which operation is executed
   */
  void executeDelete(int key);
}
