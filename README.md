# Giraffe
Kotlin networking framework targeted for the JVM

# Quick Start

## Setting up a Server
```java
//Creating the address to host the server on
SocketAddress address = new InetSocketAddress("localhost", 1234);

/*Creating the server. The second parameter is a thread dispatcher for packet processing
There are many different PacketProcessors to use-this is just an arbitrary example.*/
Server server = new GServer(address, new CoroutineDispatcherPacketProcessor(Dispatchers.getDefault()));

//Tells the server to start accepting clients
server.enable();


```

## Setting up a Client

```java


```
