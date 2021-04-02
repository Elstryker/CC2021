import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpGw {
    public static void main( String[] args ) throws IOException {
        FSChunk protocol = new FSChunk();
        protocol.start();
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(new GatewayWorker(client,protocol)).start();
            }
        }
    }
}
