package coordinator;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import server.TwoPCServer;
import service.RpcService;

public class Coordinator implements TwoPCCoordinator {

  List<TwoPCServer> dataStores;
  ExecutorService executorService = Executors.newFixedThreadPool(10);

  public Coordinator() {
    this.dataStores = new ArrayList<>();
  }

  public void registerNewServer(TwoPCServer server) {
    dataStores.add(server);
  }

  @Override
  public String twoPhaseProtocol(String operation, int key, String value) {
      // Unique ID generated for the connected client
      UUID clientId = UUID.randomUUID();
      if(!executePreparePhase(operation, key, clientId)){
        System.out.println("Server is busy. Please try again later.");
        System.out.println("Aborting operation...");
        return "400";
      }
      else if(!executeCommitPhase(operation, key, value, clientId)){
        System.out.println("Error occurred during commit");
        System.out.println("Aborting commit operation...");
        return "400";
      }

    if(operation.equals("GET")){
      return "200";
    }
    else if(operation.equals("PUT")){
      // Execute PUT request on all server replicas
      for(TwoPCServer store: dataStores) {
        executorService.submit(() -> {
          store.executePut(key, value);
        });
      }
    }
    else{
      // Execute DELETE request on all server replicas
      for(TwoPCServer store: dataStores) {
        executorService.submit(() -> {
          store.executeDelete(key);
        });
      }
    }
      return "200";
  }

  private boolean executeCommitPhase(String operation, int key, String value, UUID clientId) {
    List<Future<Boolean>> futures = new ArrayList<>();
    System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
            + " Initiating commit phase for all servers...");

    // Initiate commit phase on all replicas
    for(TwoPCServer store: dataStores) {
      Future<Boolean> future = executorService.submit(() -> {
        return store.confirmCommit(operation, key, value, clientId);
      });
      futures.add(future);
    }

    int count = 0;
    for(Future<Boolean> future: futures){
      try{
        if(future.get().equals(true)){
          count += 1;
        }
        else {
          Thread.sleep(2000);
          if(future.get().equals(true)){
            count += 1;
          }
        }
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }

    // If all servers are ready, return true
    return count == dataStores.size();
  }

  private boolean executePreparePhase(String operation, int key, UUID clientId){
    List<Future<Boolean>> futures = new ArrayList<>();
    System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
            + " Initiating prepare phase for all servers...");

    // Initiate prepare phase on all server replicas
    for(TwoPCServer store: dataStores) {
        Future<Boolean> future = executorService.submit(() -> {
          return store.confirmPrepare(operation, key, clientId);
        });
        futures.add(future);
    }

    int count = 0;
    for(Future<Boolean> future: futures){
      try{
        if(future.get().equals(true)){
          count += 1;
        }
        else {
          Thread.sleep(2000);
          if(future.get().equals(true)){
            count += 1;
          }
        }
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }

    // return true if all servers are prepared
    return count == dataStores.size();
  }
}
