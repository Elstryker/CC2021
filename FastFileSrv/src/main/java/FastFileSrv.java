import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FastFileSrv {
    private final DatagramSocket socket;
    private final ExecutorService threadPool;

    public FastFileSrv() throws SocketException {
        socket = new DatagramSocket();
        threadPool = Executors.newFixedThreadPool (100);
    }

    public static void main(String[] args) throws Exception {
        FastFileSrv server = new FastFileSrv();
        System.out.println("Starting authentication");
        if(server.authenticate()) {
            System.out.println("Authentication successful");
            server.service();
        } else {
            System.out.println("Authentication failed");
        }
    }

    public boolean authenticate() throws Exception {
        ServerAuthenticator serverAuthenticator = new ServerAuthenticator(socket);
        return serverAuthenticator.authenticateServer();
    }


    private void service() throws IOException {
        Runnable quitter = new Quitter();
        threadPool.execute(quitter);
        while (true) {
            Runnable requester = new RequestHandler(socket);
            /*
             * Use thread pool to allocate idle threads for processing
             * Currently connected client
             */
            threadPool.execute(requester);
        }
    }
}