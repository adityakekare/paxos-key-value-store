package server;

import java.io.Serializable;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import coordinator.Coordinator;
import coordinator.PaxosCoordinator;
import paxos.Operation;
import paxos.Promise;
import paxos.Proposal;
import paxos.RequestMethod;
import paxos.Status;
import service.RpcService;

import static paxos.PaxosUtils.createProposal;

/**
 * A multi-threaded server that implements the Key-Value Store.
 */
public class KeyValueStoreServer implements PaxosServer, Serializable {

  private int maxId;
  private Proposal acceptedProposal;
  private final Map<Integer, String> keyValueStore;
  private final PaxosCoordinator cd;
  private final int serverId;
  private final int port;

  public KeyValueStoreServer(PaxosCoordinator coordinator, int serverId, int port) throws RemoteException {
    super();
    maxId = 0;
    acceptedProposal = null;
    this.port = port;
    cd = coordinator;
    this.serverId = serverId;
    // Register server into coordinator
    cd.registerNewServer(this);
    // Concurrent hashmap so that server can handle concurrent requests
    keyValueStore = new ConcurrentHashMap<>();
  }

  public int getPort() {
    return port;
  }

  @Override
  public Promise prepare(Proposal proposal) {
    System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
            + " Server " + serverId + ": Received a Prepare message");
    if (Math.random() < 0.01) {
      System.out.println("Server " + serverId + ": Server Failure");
      return null;
    }
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    FutureTask<Promise> future = new FutureTask<>(new Callable<Promise>() {
      @Override
      public Promise call() throws Exception {
        if(maxId == 0){
          KeyValueStoreServer.this.maxId = proposal.getId();
          return new Promise(Status.PROMISED, proposal);
        }
        else if (proposal.getId() <= KeyValueStoreServer.this.maxId) {
          return new Promise(Status.REJECTED, null);
        } else {
          KeyValueStoreServer.this.maxId = proposal.getId();
          if (KeyValueStoreServer.this.acceptedProposal != null) {
            return new Promise(Status.ACCEPTED, new
                    Proposal(KeyValueStoreServer.this.acceptedProposal.getId(),
                    KeyValueStoreServer.this.acceptedProposal.getOperation()));
          } else {
            return new Promise(Status.PROMISED, proposal);
          }
        }
      }
    });

    try {
      executorService.submit(future);
      return future.get(20, TimeUnit.SECONDS);
    } catch (Exception e) {
      System.out.println("Error");
      return null;
    }
  }

  @Override
  public Boolean accept(Proposal proposal) {
    System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
            + " Server " + serverId + ": Received a Accept message");
    if (Math.random() < 0.01) {
      System.out.println("Server " + serverId + ": Server Failure");
      return null;
    }

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    FutureTask<Boolean> future = new FutureTask<>(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        if (proposal.getId() != KeyValueStoreServer.this.maxId) {
          return Boolean.FALSE;
        }

        if (acceptedProposal == null) {
          acceptedProposal = new Proposal(proposal.getId(), proposal.getOperation());
        } else {
          acceptedProposal.setId(proposal.getId());
          acceptedProposal.setOperation(proposal.getOperation());
        }
        return Boolean.TRUE;
      }
    });

    try {
      executorService.submit(future);
      return future.get(20, TimeUnit.SECONDS);
    } catch (Exception e) {
      System.out.println("Error");
      return null;
    }

  }

  @Override
  public String learn(Proposal proposal) {
    System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
            + " Server " + serverId + ": Received a Learn message");
    int key = proposal.getOperation().getKey();
    RequestMethod method = proposal.getOperation().getMethod();
    switch (method) {
      case GET:
        if (!keyValueStore.containsKey(key)) {
          acceptedProposal = null;
          return "Key " + key + " not found. GET request aborted.";
        }
        return executeGet(proposal.getOperation().getKey());
      case PUT:
        executePut(key, proposal.getOperation().getValue());
        acceptedProposal = null;
        return "Key " + key + " was successfully inserted";
      case DEL:
        if (!keyValueStore.containsKey(key)) {
          acceptedProposal = null;
          return "Key " + key + " not found. GET request aborted.";
        }
        executeDelete(key);
        acceptedProposal = null;
        return "Key " + key + " successfully deleted";
      default:
        acceptedProposal = null;
        return "Invalid Input.";
    }
  }

  @Override
  public String get(int key) throws RemoteException {
    // Logging
    System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
            + " Server " + serverId + ": Received GET request from client");

    Operation operation = new Operation(RequestMethod.GET, key);
    Proposal proposal = createProposal(operation);
    //GET operation
    return this.cd.executeOperation(proposal);
  }

  @Override
  public String put(int key, String value) throws RemoteException {
    // Logging
    System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
            + " Server " + serverId + ": Received PUT request from client");

    Operation operation = new Operation(RequestMethod.PUT, key, value);
    Proposal proposal = createProposal(operation);

    return this.cd.executeOperation(proposal);
  }

  @Override
  public String delete(int key) throws RemoteException {
    // Logging
    System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
            + " Server " + serverId + ": Received DELETE request from client");

    Operation operation = new Operation(RequestMethod.DEL, key);
    Proposal proposal = createProposal(operation);

    return this.cd.executeOperation(proposal);
  }

  public String executeGet(int key) {
    return keyValueStore.get(key);
  }

  public void executePut(int key, String value) {
    keyValueStore.put(key, value);
  }

  public void executeDelete(int key) {
    keyValueStore.remove(key);
  }

  public static void main(String[] args) throws RemoteException {

    List<Integer> listOfPorts = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      listOfPorts.add(Integer.parseInt(args[i]));
    }
    // Create registry
    Registry registry = LocateRegistry.createRegistry(1099);

    //Create Coordinator
    PaxosCoordinator cd = new Coordinator();

    // Instantiate 5 servers on different ports
    for (int i = 0; i < 5; i++) {
      // Server instance
      PaxosServer server = new KeyValueStoreServer(cd, i + 1, listOfPorts.get(i));

      // Remote stub that is used for RPC communication. 0 port indicates any port is fine
      PaxosServer stub = (PaxosServer) UnicastRemoteObject.exportObject(server, listOfPorts.get(i));

      try {
        // Bind the remote object to the registry for RPC communication
        registry.bind("KeyValueStore" + (i + 1), stub);
        System.out.println("Server " + (i + 1) + " ready...");
      } catch (AlreadyBoundException e) {
        System.err.println("Remote object could not be bound");
      }

    }

  }
}
