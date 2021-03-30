import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class HttpGw {
    public static void main( String[] args ) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(new GatewayWorker(client)).start();
            }
        }
    }
}
