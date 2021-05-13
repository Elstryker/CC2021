import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FastFileSrv {
    private final DatagramSocket socket;
    private static String listener;
    private final ExecutorService threadPool;

    public FastFileSrv() throws SocketException {
        socket = new DatagramSocket();
        threadPool = Executors.newFixedThreadPool (100);
    }

    public static void main(String[] args) throws IOException {

        try {
            if (args[0] != null) {
                System.out.println ("Will wait for " + args[0]);
                listener = args[0];
            }
        }catch (ArrayIndexOutOfBoundsException e) {
            ;
        }

        FastFileSrv server = new FastFileSrv ();
        System.out.println ("Starting authentication");

        if (server.authenticate ()) {
            System.out.println ("Authentication complete");
            if(listener!=null)
                System.out.println ("Listening "+listener);
            server.service ();
        } else {
            System.out.println ("Authentication failed");
        }
    }

    /*
        Authenticates the server. If the auth was successful the port number must be != 0
     */
    public boolean authenticate() throws UnknownHostException {
        ServerAuthenticator serverAuthenticator = new ServerAuthenticator(socket,listener);
        return serverAuthenticator.authenticateServer();
    }



    private void service() throws IOException {
        Runnable quitter = new Quitter(socket.getLocalPort());
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