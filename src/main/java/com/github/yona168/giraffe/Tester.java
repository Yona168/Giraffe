package com.github.yona168.giraffe;

import com.github.yona168.giraffe.net.messenger.client.Client;
import com.github.yona168.giraffe.net.messenger.packetprocessor.CoroutineDispatcherPacketProcessor;
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor;
import com.github.yona168.giraffe.net.messenger.packetprocessor.SingleThreadPacketProcessor;
import com.github.yona168.giraffe.net.messenger.server.GServer;
import com.github.yona168.giraffe.net.messenger.server.Server;
import com.github.yona168.giraffe.net.packet.PacketsKt;
import com.github.yona168.giraffe.net.packet.QueuedOpPacket;
import com.github.yona168.giraffe.net.packet.SendablePacket;
import kotlin.Unit;
import kotlinx.coroutines.Dispatchers;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

public class Tester {
    public static void main(String[] args) {
        SocketAddress address = new InetSocketAddress("localhost", 1234);
        Server server = new GServer(address, new CoroutineDispatcherPacketProcessor(Dispatchers.getDefault()));
        (server).enable();
        Client client = new Client(new SingleThreadPacketProcessor());
        SendablePacket packet = PacketsKt.packetBuilder((short)3, sendablePacket -> {
            sendablePacket.writeInt(45635);
            sendablePacket.writeString("Hello working program!");
        });
        client.registerHandler((short) 3, (receivedPacket, writable) -> {
            System.out.println(receivedPacket.readInt() + " " + receivedPacket.readString());
        });
        ((GServer) server).registerHandler((short)2, ((receivablePacket, writable) -> {
            System.out.println(receivablePacket.readString());
        }));
        client.onHandshake((uuidPacket, writable) -> {
            System.out.println("Received Handshake!");
            server.sendToAllClients(packet);
            final UUID sessionUUID=new UUID(uuidPacket.readLong(), uuidPacket.readLong());
            server.sendToClient(sessionUUID, packet);
            client.write(PacketsKt.packetBuilder((short)2, toServerPacket->{
                toServerPacket.writeString("I am the server!");
            }));
        });
        client.enableOnConnect();
        client.connectBlocking(address);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client.disable();
        System.out.println("Ending program");

    }

}
