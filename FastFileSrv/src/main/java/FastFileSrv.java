import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FastFileSrv {
    private final DatagramSocket socket;
    private final ExecutorService threadPool;
    // port to which the datagram socket is connected to in the httpGw side.
    // Must be known to send a quit request form another socket.
    private int mainPort;

    public FastFileSrv() throws SocketException {
        socket = new DatagramSocket();
        threadPool = Executors.newFixedThreadPool (100);
        mainPort = 0;
    }

    public static void main(String[] args) throws IOException {
        FastFileSrv server = new FastFileSrv();
        System.out.println("Starting authentication");

        if(server.authenticate()) {
            System.out.println("Authentication successful, on port: " +server.mainPort);
            server.service();
        } else {
            System.out.println("Authentication failed");
        }
    }

    /*
        Authenticates the server. If the auth was successful the port number must be != 0
     */
    public boolean authenticate() throws UnknownHostException {
        ServerAuthenticator serverAuthenticator = new ServerAuthenticator(socket);
        this.mainPort = serverAuthenticator.authenticateServer();
        return this.mainPort!=0;
    }


    private void service() throws IOException {
        Runnable quitter = new Quitter(socket, mainPort);
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