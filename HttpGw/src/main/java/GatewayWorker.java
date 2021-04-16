import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class GatewayWorker implements Runnable{

    private final Socket clientSocket;
    private FSChunk protocol;

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
                    System.out.println(request.wholeRequest);
                    System.out.println(request.version);
//                System.out.println(request.wholeRequest);
//                for (Map.Entry<String, String> key_value: request.headers.entrySet()){
//                    System.out.println(key_value.getKey()+ " = " + key_value.getValue());
//                }

                    String accessLog = String.format("Client %s, method %s, path %s, version %s, host %s, headers %s",
                            clientSocket.toString(), request.method, request.path, request.version, request.host, request.headers.toString());
                    // System.out.println(accessLog);
                    System.out.println(request.file);


                    try {
                        MyPair<byte[],String> receive = protocol.retrieveFile(request.file);
                        sendResponse(clientOutput, "200 OK", receive.getSecond(),request.file, receive.getFirst());

                    } catch (FileNotFoundException e) {
                        byte[] notFoundContent = "<h1>Not found :(</h1>".getBytes();
                        sendResponse(clientOutput, "404 Not Found","text/html", request.file, notFoundContent);
                    }

                } catch (SocketTimeoutException | NullPointerException e){ //
                   // e.printStackTrace();
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


    /*
    version status code
    headers
    (empty line)
    content
    (empty line)
     */
    private static void sendResponseTest(OutputStream clientOutput, String status, String contentType, String contentName, byte[] content) throws IOException {
//        clientOutput.write(("HTTP/1.1 " + status + "\n").getBytes());
//        clientOutput.write(("ContentType: " + contentType + "\n").getBytes());
//        if(!contentType.trim().equals("text/html"))
//            clientOutput.write(("Content-Disposition: attachment; filename=\"" + contentName +"\"\n").getBytes());
//        clientOutput.write(("Content-Length: " + content.length + "\n").getBytes());
//        clientOutput.write("\n".getBytes());
//        clientOutput.write(content);
//        clientOutput.write("\n\n".getBytes());
//        clientOutput.flush();



//        clientOutput.write(("HTTP/1.1 " + status + "\n").getBytes());
//        System.out.print("HTTP/1.1 " + status + "\n");
//        clientOutput.write(("ContentType: " + contentType + "\n").getBytes());
//        System.out.print("ContentType: " + contentType + "\n");
//        if(!contentType.trim().equals("text/html"))
//            clientOutput.write(("Content-Disposition: attachment; filename=\"" + contentName +"\"\n").getBytes());
//       //clientOutput.write(("Content-Length: " + content.length + "\n").getBytes());
//        clientOutput.write(("Transfer-Encoding: chunked\n\n").getBytes());
//        System.out.print("Transfer-Encoding: chunked\n\n");
//        clientOutput.flush();
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        clientOutput.write((Integer.toHexString(content.length) + "\n").getBytes());
//        System.out.print((Integer.toHexString(content.length) + "\n"));
//        clientOutput.write(content);
//        System.out.print(Arrays.toString(content));
//        clientOutput.write("\n".getBytes());
//        System.out.print("\n");
//        clientOutput.flush();
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        clientOutput.write("0\n".getBytes());
//        System.out.print("0\n\n");
//        clientOutput.flush();

        String headers = """
                HTTP/1.1 200 OK\s
                Content-Type: text/plain
                Transfer-Encoding: chunked
                
                """;
        clientOutput.write(headers.getBytes());
        clientOutput.flush();

//        String response = "Media\nServices\n";
//        String hexSize = Integer.toHexString(response.length());
//        System.out.println("Size: "+hexSize);
//        response = hexSize + "\n" + response;
//        System.out.print("----");
//        System.out.print(response);
//        System.out.print("----");
//        clientOutput.write(response.getBytes());
//        clientOutput.flush();


        String response2 = "Services\nmedia";
        String hexSize2 = Integer.toHexString(response2.getBytes().length);
        System.out.println("Size: "+hexSize2);
        response2 = hexSize2 + "\n" + response2 +"\n";
        System.out.print("----");
        System.out.print(response2);
        System.out.print("----");
        clientOutput.write(response2.getBytes());
        clientOutput.flush();

        String response3 = "This\nis\na\ntest";
        byte[] contentBytes3 = response3.getBytes();
        String hexSize3 = Integer.toHexString(contentBytes3.length) + "\n";
        byte[] hexSizeBytes3 = hexSize3.getBytes();
        byte[] fullResponseBytes3 = new byte[hexSizeBytes3.length + contentBytes3.length + "\n".getBytes().length ];
        System.arraycopy(hexSizeBytes3, 0, fullResponseBytes3, 0, hexSizeBytes3.length);
        System.arraycopy(contentBytes3, 0, fullResponseBytes3, hexSizeBytes3.length, contentBytes3.length);
        System.arraycopy("\n".getBytes(), 0, fullResponseBytes3, hexSizeBytes3.length + contentBytes3.length , "\n".getBytes().length);
        //response3 = hexSize + "\n" + response3 + "\n";
        clientOutput.write(fullResponseBytes3);
        clientOutput.flush();

        String response= "0\n\n";
        clientOutput.write(response.getBytes());
        clientOutput.flush();
    }
}
