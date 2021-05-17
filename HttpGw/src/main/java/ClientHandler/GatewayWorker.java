package ClientHandler;

import FSChunk.FSChunk;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class GatewayWorker implements Runnable{

    private final Socket clientSocket;
    private final FSChunk protocol;

    public GatewayWorker(Socket client, FSChunk protocol){
        this.clientSocket = client;
        this.protocol = protocol;
    }

    @Override
    public void run()  {
        boolean keep_alive;
        System.out.println("Debug: got new client " + clientSocket.toString());
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream clientOutput = clientSocket.getOutputStream();
            do {
                System.out.println("Reading request!");
                try {
                    clientSocket.setSoTimeout(5000); // Will only wait 5 seconds for a new request in a persistent connection
                    HTTPRequest request = readRequest(br);
                    clientSocket.setSoTimeout(0); // disable timeout while serving the request
                    keep_alive = request.persistent;

                    protocol.retrieveAndSendFile(clientOutput, request.file);
                } catch (SocketTimeoutException | NullPointerException e){ // either the socket has timed out or the pipe has been broken
                    keep_alive = false;
                }
            } while(keep_alive);
            System.out.println("Exited");
            clientOutput.close();
            clientSocket.close();
            br.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Throws NullPointerException when the communication pipe is broken by clients such as wget and curl
    private HTTPRequest readRequest(BufferedReader br) throws IOException, NullPointerException {
        StringBuilder requestBuilder = new StringBuilder();
        String line;
        while (!(line = br.readLine()).isBlank()) {
            requestBuilder.append(line).append("\n");
        }

        String requestString = requestBuilder.toString();
        return new HTTPRequest(requestString);
    }
}
