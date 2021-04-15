import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FastFileSrv {
    private final DatagramSocket socket;
    private ExecutorService threadPool;

    public FastFileSrv() throws SocketException {
        socket = new DatagramSocket();
        byte[] aut = new byte[1];
        boolean sent = false;
        while(!sent) {
            try {
                socket.setSoTimeout(1000);
                DatagramPacket packet = new DatagramPacket(aut, aut.length, InetAddress.getByName("localhost"), 12345);
                socket.send(packet);
                System.out.println("Enviei Pacote autenticacao");
                packet = new DatagramPacket(aut, aut.length);
                System.out.println("Espero Resposta");
                socket.receive(packet);
                threadPool = Executors.newFixedThreadPool (100);
                sent = true;
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        System.out.println("Autenticado!");
        socket.setSoTimeout(0);
    }

    public static void main(String[] args) throws IOException {
        FastFileSrv server = new FastFileSrv();
        server.service();
    }

    private void service() throws IOException {
        while (true) {
             Runnable requester = new RequestHandler (socket);
            /*
             * Use thread pool to allocate idle threads for processing
             * Currently connected client
             */
            threadPool.execute(requester);
        }
    }
}