package server;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import coordinator.Coordinator;
import coordinator.TwoPCCoordinator;
import service.RpcService;

/**
 * A multi-threaded server that implements the Key-Value Store.
 */
public class KeyValueStoreServer implements RpcService, TwoPCServer {

  // Map that stores the key-value pairs
  private final Map<Integer, String> keyValueStore;
  private UUID clientId;
  private final TwoPCCoordinator cd;
  private final int serverId;

  public KeyValueStoreServer(TwoPCCoordinator coordinator, int serverId) {
    cd = coordinator;
    this.serverId = serverId;
    // Register server into coordinator
    cd.registerNewServer(this);
    // Concurrent hashmap so that server can handle concurrent requests
    keyValueStore = new ConcurrentHashMap<>();
    clientId = null;
  }

  public String executeGet(int key){
    return keyValueStore.get(key);
  }

  public void executePut(int key, String value){
    keyValueStore.put(key, value);
  }

  public void executeDelete(int key){
    keyValueStore.remove(key);
  }

  @Override
  public String get(int key) throws RemoteException {
    // Logging
    System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
            + " Server " + serverId + ": Received GET request from client");

    if(!keyValueStore.containsKey(key)) {
      return "Key " + key + " not found. GET request aborted.";
    }

    if(cd.twoPhaseProtocol("GET", key, null).equals("400")){
      return "Server is busy. Please try again later.";
    }
    //GET operation
    return this.executeGet(key);
  }

  @Override
  public String put(int key, String value) throws RemoteException {
    // Logging
    System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
            + " Server " + serverId + ": Received PUT request from client");

    if(cd.twoPhaseProtocol("PUT", key, value).equals("400")){
      return "Server is busy. Please try again later.";
    }

    return "Key " + key + " was successfully inserted";
  }

  @Override
  public String delete(int key) throws RemoteException {
    // Logging
    System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
            + " Server " + serverId + ": Received DELETE request from client");

    if(!keyValueStore.containsKey(key)) {
      return "Key " + key + " not found. DELETE request aborted.";
    }

    if(cd.twoPhaseProtocol("DELETE", key, null).equals("400")){
      return "Server is busy. Please try again later.";
    }

    return "Key " + key + " successfully deleted";
  }

  @Override
  public boolean confirmPrepare(String operation, int key, UUID cid) {
    System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
            + " Server " + serverId + ": Prepare phase initiated for " + operation
            + " request on " + "key " + key);
    // if clientId is null, no client is currently connected and hence the server is prepared
    if(clientId == null){
      clientId = cid;
      return true;
    }
    return false;
  }

  @Override
  public boolean confirmCommit(String operation, int key, String value, UUID cid) {
    System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
            + " Server " + serverId + ": Commit phase initiated for " + operation
            + " request on " + key + " key");
    // Check if the client making the commit is the same that made the prepare request
    if(clientId == cid){
      //commit
      clientId = null;
      return true;
    }
    return false;
  }

  public static void main(String[] args) throws RemoteException {

    List<Integer> listOfPorts = new ArrayList<>();
    for(int i = 0; i < 5; i++){
      listOfPorts.add(Integer.parseInt(args[i]));
    }
    // Create registry
    Registry registry = LocateRegistry.createRegistry(1099);

    //Create Coordinator
    TwoPCCoordinator cd = new Coordinator();

    // Instantiate 5 servers on different ports
    for(int i = 0; i < 5; i++) {
      // Server instance
      KeyValueStoreServer server = new KeyValueStoreServer(cd, i + 1);

      // Remote stub that is used for RPC communication. 0 port indicates any port is fine
      RpcService stub = (RpcService) UnicastRemoteObject.exportObject(server, listOfPorts.get(i));

      try {
        // Bind the remote object to the registry for RPC communication
        registry.bind("KeyValueStore" + (i + 1), stub);
        System.out.println("Server " + (i+1) + " ready...");
      } catch (AlreadyBoundException e) {
        System.err.println("Remote object could not be bound");
      }

    }

  }
}
