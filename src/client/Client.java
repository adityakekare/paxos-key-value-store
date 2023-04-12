package client;

import java.io.DataInputStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLOutput;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import service.RpcService;

/**
 * Client class for RPC communication
 */
public class Client {

  /**
   * Private method to parse user input for operations on server
   * @param input String input from user
   * @return List of strings containing operation, key, value
   */
  private static List<String> parseInput(String input) {
    if(input.length() < 6){
      return null;
    }
    String toParse = input.strip();
    String method = toParse.substring(0, 3).toUpperCase();
    int key = -1;
    String value = "";
    List<String> results = new ArrayList<>();
    if (method.compareTo("GET") == 0) {
      try {
        key = Integer.parseInt(input.replaceAll("[^0-9]", ""));
      } catch (NumberFormatException nfe) {
        return null;
      }
    } else if (method.compareTo("PUT") == 0) {
      char[] arr = input.toCharArray();
      int openBracketIdx = input.indexOf("(");
      int commaIdx = input.indexOf(",");
      int openQuotesIdx = input.indexOf("\"");
      int closeQuotesIdx = input.indexOf("\"", openQuotesIdx + 1);

      if (openBracketIdx > 0 && commaIdx > 0 && openQuotesIdx > 0 && closeQuotesIdx > 0) {
        try {
          key = Integer.parseInt(input.substring(openBracketIdx + 1, commaIdx));
        } catch (NumberFormatException nfe) {
          return null;
        }
        value = input.substring(openQuotesIdx + 1, closeQuotesIdx);
      }
    } else if (method.compareTo("DEL") == 0) {
      if (toParse.substring(0, 6).toUpperCase().compareTo("DELETE") == 0) {
        try {
          key = Integer.parseInt(input.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException nfe) {
          return null;
        }
      } else {
        return null;
      }
    }

    results.add(method);
    results.add(Integer.toString(key));
    results.add(value);

    return results;
  }

  /**
   * Private method to pre-populate the server
   * @param stub RpcService object for RPC communication
   */
  private static void prePopulate(RpcService stub) {
    int[] keys = {1,2,3,4,5,6,7,8,9,10};
    String[] names = {"abc", "bcd", "cde", "edf", "dfg", "ghi", "jkl", "mno", "pqr", "stu"};

    for(int i = 0; i < 10; i++){
      try {
        System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
                + " Server response: " + stub.put(keys[i], names[i]));
      } catch (RemoteException error) {
        System.err.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
                + " Client exception " + error.toString());
      }
    }
  }

  public static void main(String[] args) {
    Registry registry = null;
    try {
      // Locates the registry
      registry = LocateRegistry.getRegistry(null);

      // Lookup the server remote object in the registry
      RpcService stub = (RpcService) registry.lookup("KeyValueStore1");

      // Pre-populate key-value store and perform 5 PUT operations
      prePopulate(stub);

      // 5 GET and 5 DELETE operations
      for(int i = 0; i < 5; i++) {
          // Logging
          System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
                  + " Server response: " + stub.get(i));
          System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
                  + " Server response: " + stub.delete(i));
      }

      // Take input for which server to connect to
      DataInputStream input = new DataInputStream(System.in);
      String line = "";
      System.out.println("Enter the server you want to connect to (1-5):");
      line = input.readLine();
      int server = Integer.parseInt(line);

      // Server name in registry: KeyValueStore1, KeyValueStore2, etc.
      stub = (RpcService) registry.lookup("KeyValueStore" + server);
      System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
              + " Connected to server " + server);
      while(true){
        line = input.readLine();
        List<String> results = parseInput(line);
        if(results == null){
          System.out.println("Invalid Input.");
          continue;
        }
        String method = results.get(0);
        int key = Integer.parseInt(results.get(1));
        String value = results.get(2);

        switch (method) {
          case "GET":
            System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
                    + " Server response: " + stub.get(key));
            break;
          case "PUT":
            System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
                    + " Server response: " + stub.put(key, value));
            break;
          case "DEL":
            System.out.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
                    + " Server response: " + stub.delete(key));
            break;
          default:
            System.out.println("Invalid Input.");
            break;
        }
      }
    } catch (Exception error) {
      System.err.println("[" + new Timestamp(new Date().getTime()).toString() + "]"
              + " Client exception " + error.toString());
    }
  }
}
