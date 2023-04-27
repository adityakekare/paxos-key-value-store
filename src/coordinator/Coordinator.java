package coordinator;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

import paxos.Promise;
import paxos.Proposal;
import paxos.Status;
import server.PaxosServer;

public class Coordinator implements PaxosCoordinator, Serializable {

  List<PaxosServer> dataStores;

  public Coordinator() {
    this.dataStores = new ArrayList<>();
  }

  public void registerNewServer(PaxosServer server) {
    dataStores.add(server);
  }

  @Override
  public String executeOperation(Proposal proposal) throws RemoteException {
    Registry registry = null;
    List<PaxosServer> acceptors = new ArrayList<>();
    // List to store servers that are down
    List<PaxosServer> failedServers = new ArrayList<>();
    // Locates the registry
    registry = LocateRegistry.getRegistry(null);


    int i = 1;
    for(PaxosServer server: dataStores){
      try{
        // Lookup the server remote object in the registry
        PaxosServer acceptor = (PaxosServer) registry.lookup("KeyValueStore" + i);
        acceptors.add(acceptor);
        i += 1;
      } catch (NotBoundException e) {
        System.out.println("Server at port " + server.getPort() + " is down");
      }
    }

    int half = Math.floorDiv(acceptors.size(), 2) + 1;
    int numPromised = 0;

    for(PaxosServer acceptor: acceptors){
      try{
        Promise promise = acceptor.prepare(proposal);
        if(promise == null){
          System.out.println("Server at port " + acceptor.getPort() + " is down");
          failedServers.add(acceptor);
        }
        else if(promise.getStatus() == Status.PROMISED || promise.getStatus() == Status.ACCEPTED){
          numPromised += 1;
          System.out.println("Server at port " + acceptor.getPort() + " has PROMISED proposal " +
                  proposal.getOperation().getMethod() + "(" + proposal.getOperation().getKey() +
                  "," + proposal.getOperation().getValue() + ")");
        }
        else{
          System.out.println("Server at port " + acceptor.getPort() + " has REJECTED proposal " +
                  proposal.getOperation().getMethod() + "(" + proposal.getOperation().getKey() +
                  "," + proposal.getOperation().getValue() + ")");
        }
      } catch (Exception e) {
        System.out.println("Server at port " + acceptor.getPort() + " is NOT RESPONDING to the proposal " +
                proposal.getOperation().getMethod() + "(" + proposal.getOperation().getKey() +
                "," + proposal.getOperation().getValue() + ")");
      }
    }

    // Check the majority
    if(numPromised < half){
      return "Consensus not reached";
    }

    // Remove servers that are down from the acceptors list
    for(PaxosServer acceptor: failedServers){
      acceptors.remove(acceptor);
    }
    // Empty the servers that are down list
    failedServers.clear();


    int accepted = 0;
    for(PaxosServer acceptor: acceptors){
      try{
        Boolean isAccepted = acceptor.accept(proposal);

        if(isAccepted == null){
          System.out.println("Server at port " + acceptor.getPort() + " is NOT RESPONDING to the proposal " +
                  proposal.getOperation().getMethod() + "(" + proposal.getOperation().getKey() +
                  "," + proposal.getOperation().getValue() + ")");
          failedServers.add(acceptor);
        }
        else if(isAccepted){
          accepted += 1;
          System.out.println("Server at port " + acceptor.getPort() + " has ACCEPTED proposal " +
                  proposal.getOperation().getMethod() + "(" + proposal.getOperation().getKey() +
                  "," + proposal.getOperation().getValue() + ")");

        }
      } catch (Exception ignored) {
      }
    }

    // Check the majority
    if(accepted < half){
      return "Consensus not reached";
    }

    // Remove servers that are down from the acceptors list
    for(PaxosServer acceptor: failedServers){
      acceptors.remove(acceptor);
    }

    String result = "";

    for(PaxosServer acceptor: acceptors){
      try{
        result = acceptor.learn(proposal);
      } catch (Exception ignored) {
      }
    }

    return result;
  }
}
