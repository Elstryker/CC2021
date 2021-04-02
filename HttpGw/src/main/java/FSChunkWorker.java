import java.io.IOException;
import java.net.*;

public class FSChunkWorker {
    private DatagramSocket socket;
    private DatagramPacket packet;
    private InetAddress serverAddress;
    private int serverPort;
    private byte[] data;
    private int maxLength = 20 * 1024;

    public FSChunkWorker(DatagramSocket sock, InetAddress serverAddress, Integer serverPort) {
        socket = sock;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public byte[] getMetaData(String file) {
        data = ("INFO " + file).getBytes();
        return this.getPacket();
    }

    public byte[] getFile(String file, int offset, int size) {
        data = (String.format("GET %d %d %s",offset,size,file)).getBytes();
        return this.getPacket();
    }

    private byte[] getPacket() {
        boolean sent = false;
        byte[] receivedBytes = new byte[maxLength];
        try {
            // Limita-se a esperar apenas 1 segundo por chegadas de pacotes
            socket.setSoTimeout(1000);
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }
        // Ciclo para controlo de timeouts
        while(!sent) {
            try {
                // Envia pedido do pacote
                packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                socket.send(packet);
                // Receção do pacote
                packet = new DatagramPacket(receivedBytes, receivedBytes.length);
                socket.receive(packet);
                sent = true;
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
        return data;
    }
}
