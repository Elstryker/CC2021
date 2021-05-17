package FSChunk;

import Utils.MyPair;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FSChunkWorker {
    private final DatagramSocket socket;
    private final String file;
    private final ArrayList<MyPair<InetAddress,Integer>> servers;
    private int serverPointer;

    public FSChunkWorker(DatagramSocket sock, String file, InetAddress serverAddress, Integer serverPort) {
        socket = sock;
        this.servers = new ArrayList<>();
        this.servers.add(new MyPair<>(serverAddress,serverPort));
        this.file = file;
        this.serverPointer = -1;
    }

    public FSChunkWorker(DatagramSocket sock, String file, HashMap<InetAddress, ArrayList<Integer>> servers) {
        socket = sock;
        this.file = file;
        this.servers = new ArrayList<>();
        for(Map.Entry<InetAddress,ArrayList<Integer>> entry : servers.entrySet())
            for (Integer port : entry.getValue())
                this.servers.add(new MyPair<>(entry.getKey(),port));
        this.serverPointer = 0;
    }

    public FileMetaData getMetaData() throws NoSuchFieldException {
        if (this.file.equals(""))
            throw new NoSuchFieldException("No file especified");
        byte[] dataRequested = this.requestData("INFO " + this.file,("INFO " + this.file).getBytes(), 5000); // Random value bigger than the occupied by the metadata
        return new FileMetaData(new String(dataRequested));
    }

    public byte[] getFile(long offset, int size) throws NoSuchFieldException {
        if (this.file.equals(""))
            throw new NoSuchFieldException("No file specified");
        String request = (String.format("GET %d %d %s",offset,size,this.file));
        //System.out.println(request);
        return requestData(request,(String.format("GET %d %d %s",offset,size,this.file)).getBytes(), size);
    }

    private byte[] requestData(String request, byte[] message, int packetSize) {
        boolean authCompleted = false;
        byte[] receivedBytes = new byte[packetSize];
        DatagramPacket receivePacket = null, sendPacket;
        try {
            // Timeout will trigger when he waits 1 second and didn't receive any packet
            socket.setSoTimeout(1000);
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }
        // Timeout control cycle
        while(!authCompleted) {
            try {
                // Get Server to send
                MyPair<InetAddress,Integer> destinationServer;
                // If pointer equals -1 means that it does not need round robin technique and selects the only server available
                if(serverPointer == -1)
                    destinationServer = servers.get(0);
                else {
                    destinationServer = servers.get(serverPointer);
                    serverPointer = (serverPointer + 1) % servers.size();
                }
                // Send Packet
                sendPacket = new DatagramPacket(message, message.length, destinationServer.getFirst(), destinationServer.getSecond());
                socket.send(sendPacket);
                // Receive Packet
                receivePacket = new DatagramPacket(receivedBytes, receivedBytes.length);
                socket.receive(receivePacket);
                // If it reaches here it's because he received the packet and exits the cycle
                authCompleted = true;
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.out.println("Failed on: " + request);
            }
        }
        // Get only useful info from the received buffer packet
        byte[] responseData = new byte[receivePacket.getLength()];
        System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), responseData, 0, receivePacket.getLength());
        return responseData;
    }
}
