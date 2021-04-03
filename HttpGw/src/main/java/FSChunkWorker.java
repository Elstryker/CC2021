import java.io.IOException;
import java.net.*;

public class FSChunkWorker {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private String file;
    private int serverPort;
    private int maxLength = 1050;

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
            throw new NoSuchFieldException("No file especified");
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
                // Send Packet
                sendPacket = new DatagramPacket(message, message.length, serverAddress, serverPort);
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
