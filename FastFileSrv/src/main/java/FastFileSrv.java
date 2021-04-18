import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FastFileSrv {
    private final DatagramSocket socket;
    private final ExecutorService threadPool;

    public FastFileSrv() throws SocketException {
        socket = new DatagramSocket();
        sendAuthenticationPacket();
        System.out.println("Autenticado!");
        threadPool = Executors.newFixedThreadPool (100);
    }

    public static void main(String[] args) throws IOException {
        FastFileSrv server = new FastFileSrv();
        server.service();
    }

    private void sendAuthenticationPacket() {
        byte[] aut = new byte[1];
        boolean authCompleted = false;
        while(!authCompleted) {
            try {
                socket.setSoTimeout(1000);
                DatagramPacket packet = new DatagramPacket(aut, aut.length, InetAddress.getByName("localhost"), 12345);
                socket.send(packet);
                System.out.println("Enviei Pacote autenticacao");
                packet = new DatagramPacket(aut, aut.length);
                System.out.println("Espero Resposta");
                socket.receive(packet);
                authCompleted = true;
                socket.setSoTimeout(0);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void service() throws IOException {
        Runnable quitter = new Quitter ();
        threadPool.execute (quitter);
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