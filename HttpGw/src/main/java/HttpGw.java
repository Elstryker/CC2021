import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.*;
import io.github.cdimascio.dotenv.Dotenv;

import ClientHandler.GatewayWorker;
import FSChunk.*;


public class HttpGw {
    public static void main( String[] args ) throws SocketException, NoSuchAlgorithmException {
        FSChunk protocol = new FSChunk();
        protocol.start();

        // Starting HTTP Server
        new Thread(()-> {
            try (ServerSocket serverSocket = new ServerSocket(8080)) {
                System.out.println("HTTP Server setup");
                while (true) {
                    Socket client = serverSocket.accept();
                    System.out.println("New socket");
                    new Thread(new GatewayWorker(client, protocol)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Starting HTTPS Server
        new Thread(()-> {
            SSLServerSocket SSLServerSocket = null;
            try {
                SSLServerSocket = getSSLServerSocket();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(SSLServerSocket!=null) {
                System.out.println("HTTPS Server setup");
                try {
                    while (true) {
                        Socket client = SSLServerSocket.accept();
                        new Thread(new GatewayWorker(client, protocol)).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                System.out.println("Unable to setup HTTPS Server");
            }
        }).start();
    }

        private static SSLServerSocket getSSLServerSocket() throws Exception {
            // set up key manager to do server authentication
            Dotenv dotenv = Dotenv.configure()
                    .directory("src/main/security")
                    .load();
            String keystorePassword = dotenv.get("KEYSTORE_PASSWORD");

            char[] passphrase = keystorePassword.toCharArray();
            SSLContext ctx = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

            ks.load(new FileInputStream("src/main/security/server.keystore"), passphrase);
            kmf.init(ks, passphrase);
            ctx.init(kmf.getKeyManagers(), null, null);

            return (SSLServerSocket)ctx.getServerSocketFactory().createServerSocket(8081);
        }
}
