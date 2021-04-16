package FSChunk;

import Utils.MyPair;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FSChunkWorker {
    private DatagramSocket socket;
    private String file;
    private ArrayList<MyPair<InetAddress,Integer>> servers;
    private int maxLength = 1050;
    private int serverPointer;

    public FSChunkWorker(DatagramSocket sock, InetAddress serverAddress, Integer serverPort) {
        socket = sock;
        this.servers = new ArrayList<>();
        this.servers.add(new MyPair<>(serverAddress,serverPort));
        this.file = "";
        this.serverPointer = -1;
    }

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

    public FileMetaData getMetaData(String file) {
        this.file = file;
        byte[] dataRequested = this.requestData(("INFO " + this.file).getBytes());
        return new FileMetaData(new String(dataRequested));
    }

    public FileMetaData getMetaData() throws NoSuchFieldException {
        if (this.file.equals(""))
            throw new NoSuchFieldException("No file especified");
        byte[] dataRequested = this.requestData(("INFO " + this.file).getBytes());
        return new FileMetaData(new String(dataRequested));
    }

    public byte[] getFile(String file, int offset, int size) {
        this.file = file;
        return this.requestData((String.format("GET %d %d %s",offset,size,this.file)).getBytes());
    }

    public byte[] getFile(int offset, int size) throws NoSuchFieldException {
        if (this.file.equals(""))
            throw new NoSuchFieldException("No file specified");
        return this.requestData((String.format("GET %d %d %s",offset,size,this.file)).getBytes());
    }

    private byte[] requestData(byte[] message) {
        boolean sent = false;
        byte[] receivedBytes = new byte[maxLength];
        DatagramPacket receivePacket = null, sendPacket;
        try {
            // Timeout will trigger when he waits 1 second and didn't receive any packet
            socket.setSoTimeout(1000);
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }
        // Timeout control cycle
        while(!sent) {
            try {
                // Get Server to send
                MyPair<InetAddress,Integer> destinationServer;
                // If pointer equals -1 means that it does not need round robin technique and selects the only server available
                if(serverPointer == -1)
                    destinationServer = servers.get(0);
                else {
                    destinationServer = servers.get(serverPointer);
                    if (serverPointer == servers.size()-1)
                        serverPointer = 0;
                    else serverPointer++;
                }
                // Send Packet
                sendPacket = new DatagramPacket(message, message.length, destinationServer.getFirst(), destinationServer.getSecond());
                socket.send(sendPacket);
                // Receive Packet
                receivePacket = new DatagramPacket(receivedBytes, receivedBytes.length);
                socket.receive(receivePacket);
                // If it reaches here it's because he received the packet and exits the cycle
                sent = true;
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        // Get only useful info from the received buffer packet
        byte[] responseData = new byte[receivePacket.getLength()];
        System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), responseData, 0, receivePacket.getLength());
        return responseData;
    }
}
