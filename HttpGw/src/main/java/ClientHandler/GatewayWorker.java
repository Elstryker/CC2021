package ClientHandler;

import FSChunk.FSChunk;
import Utils.MyPair;

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
                    clientSocket.setSoTimeout(10000); // Will only wait 5 seconds for a new request in a persistent connection
                    HTTPRequest request = readRequest(br);
                    clientSocket.setSoTimeout(0); // disable timeout while serving the request

                    keep_alive = request.persistent;

                    try {
                        MyPair<byte[],String> receive = protocol.retrieveFile(request.file);
                        sendResponse(clientOutput, "200 OK", receive.getSecond(),request.file, receive.getFirst());

                    } catch (FileNotFoundException e) {
                        byte[] notFoundContent = "<h1>Not found :(</h1>".getBytes();
                        sendResponse(clientOutput, "404 Not Found","text/html", request.file, notFoundContent);
                    }
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

    /*
     version status code
     headers
     (empty line)
     5\n
     Media\n
     8\n
     Services\n
     4\n
     Live\n
     0\n
     \n
    */
    private static void sendResponse(OutputStream clientOutput, String status, String contentType, String contentName, byte[] content) throws IOException {
        clientOutput.write(("HTTP/1.1 " + status + "\n").getBytes());
        clientOutput.write(("ContentType: " + contentType + "\n").getBytes());
        if(!contentType.trim().equals("text/html"))
            clientOutput.write(("Content-Disposition: attachment; filename=\"" + contentName +"\"\n").getBytes());
        clientOutput.write(("""
                Transfer-Encoding: chunked

                """).getBytes());
        clientOutput.flush();

        byte[] hexSizeBytes = (Integer.toHexString(content.length) +"\n").getBytes();
        byte[] chunk = new byte[hexSizeBytes.length + content.length + "\n".getBytes().length];

        System.arraycopy(hexSizeBytes, 0, chunk, 0, hexSizeBytes.length);
        System.arraycopy(content, 0, chunk, hexSizeBytes.length, content.length);
        System.arraycopy("\n".getBytes(), 0, chunk, hexSizeBytes.length + content.length , "\n".getBytes().length);
        clientOutput.write(chunk);
        clientOutput.flush();

        String response= "0\n\n";
        clientOutput.write(response.getBytes());
        clientOutput.flush();
    }
}
