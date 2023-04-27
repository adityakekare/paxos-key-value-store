## Java RMI multi-threading server for RPC communication replicated across 5 distinct servers

## Steps to run:
1) First, run server.jar from the root directory of the project using the command 
   (you can use different ports):
   ````
   java -jar out/artifacts/server_jar/rmi-multi-threading.jar 5000 5001 5002 5003 5004
   ````
2) Run client.jar from the root directory of the project using the command:
   ````
   java -jar out/artifacts/client_jar/rmi-multi-threading.jar
   ````
### All servers log their outputs on the same terminal. The logs come with the server's ID.
First, data will be pre-populated. Then the client will perform 5 PUT operations to pre-populate.
Then 5 PUT, 5 GET, and 5 DELETE operations will be performed.
Logs can be viewed on the console.

User can then interact with the server using inputs. User can connect with any of the server
replicas (1-5).

#### Clients can perform operations by following these examples:
1) GET(2)
2) PUT(1, "apple")
3) DELETE(2)