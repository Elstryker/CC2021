import java.io.IOException;
import java.net.*;

public class FSChunkWorker {
    DatagramSocket socket;
    DatagramPacket packet;
    MyPair<InetAddress,Integer> destination;
    byte[] data;
    int maxLength = 20 * 1024;

    public FSChunkWorker(DatagramSocket sock, String message, MyPair<InetAddress,Integer> dest) {
        socket = sock;
        data = message.getBytes();
        destination = dest;
    }

    public byte[] run() {
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
                packet = new DatagramPacket(data, data.length, destination.getFirst(), destination.getSecond());
                socket.send(packet);
                // Receção do pacote
                packet = new DatagramPacket(receivedBytes, receivedBytes.length);
                socket.receive(packet);
                sent = true;
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        return packet.getData();
    }
}
