import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GatewayWorker implements Runnable{

    private final Socket clientSocket;

    public GatewayWorker(Socket client){
        this.clientSocket = client;
    }

    @Override
    public void run()  {
        System.out.println("Debug: got new client " + clientSocket.toString());
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            StringBuilder requestBuilder = new StringBuilder();
            String line;
            while (!(line = br.readLine()).isBlank()) {
                requestBuilder.append(line).append("\n");
            }

            String requestString = requestBuilder.toString();
            HTTPRequest request = new HTTPRequest(requestString);

            String accessLog = String.format("Client %s, method %s, path %s, version %s, host %s, headers %s",
                    clientSocket.toString(), request.method, request.path, request.version, request.host, request.headers.toString());
           // System.out.println(accessLog);
            System.out.println(request.file);

            Path filePath = getFilePath(request.path);
            if (Files.exists(filePath)) {
                // file exist
                String contentType = guessContentType(filePath);
                System.out.println("Content type: " + contentType);
                sendResponse(clientSocket, "200 OK", contentType, Files.readAllBytes(filePath));
            } else {
                // 404
                byte[] notFoundContent = "<h1>Not found :(</h1>".getBytes();
                sendResponse(clientSocket, "404 Not Found", "text/html", notFoundContent);
            }
        }catch (IOException e){
                e.printStackTrace();
        }
    }

    /*
    version status code
    headers
    (empty line)
    content
    (empty line)
     */
    private static void sendResponse(Socket client, String status, String contentType, byte[] content) throws IOException {
        OutputStream clientOutput =  client.getOutputStream();
        clientOutput.write(("HTTP/1.1 " + status + "\n").getBytes());
        clientOutput.write(("ContentType: " + contentType + "\n").getBytes());
        clientOutput.write(("Content-Length: " + content.length + "\n").getBytes());
        clientOutput.write("\n".getBytes());
        clientOutput.write(content);
        clientOutput.write("\n\n".getBytes());
        clientOutput.flush();
        clientOutput.close();
        client.close();
    }

    private static String guessContentType(Path filePath) throws IOException {
        return Files.probeContentType(filePath);
    }

    private static Path getFilePath(String path) {
        if ("/".equals(path)) {
            path = "index.html";
        }

        return Paths.get("src/main/resources", path);
    }
}
