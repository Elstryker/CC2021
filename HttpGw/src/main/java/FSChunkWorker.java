import java.io.IOException;
import java.net.*;

public class FSChunkWorker {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private String file;
    private int serverPort;
    private byte[] data;
    private int maxLength = 20 * 1024;

    public FSChunkWorker(DatagramSocket sock, InetAddress serverAddress, Integer serverPort) {
        socket = sock;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.file = "";
    }

    public FSChunkWorker(DatagramSocket sock, String file, InetAddress serverAddress, Integer serverPort) {
        socket = sock;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.file = file;
    }

    public byte[] getMetaData(String file) {
        this.file = file;
        data = ("INFO " + this.file).getBytes();
        return this.getPacket();
    }

    public byte[] getMetaData() throws NoSuchFieldException {
        if (this.file.equals(""))
            throw new NoSuchFieldException("No file especified");
        data = ("INFO " + this.file).getBytes();
        return this.getPacket();
    }

    public byte[] getFile(String file, long offset, long size) {
        this.file = file;
        data = (String.format("GET %d %d %s",offset,size,this.file)).getBytes();
        return this.getPacket();
    }

    public byte[] getFile(long offset, long size) throws NoSuchFieldException {
        if (this.file.equals(""))
            throw new NoSuchFieldException("No file especified");
        data = (String.format("GET %d %d %s",offset,size,this.file)).getBytes();
        return this.getPacket();
    }

    private byte[] getPacket() {
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
                // Send Packet
                sendPacket = new DatagramPacket(data, data.length, serverAddress, serverPort);
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
        data = new byte[receivePacket.getLength()];
        System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), data, 0, receivePacket.getLength());
        return data;
    }
}
