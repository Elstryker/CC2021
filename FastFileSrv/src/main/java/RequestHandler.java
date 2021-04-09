import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RequestHandler implements Runnable{
    private DatagramSocket socket;
    private String command ;
    private InetAddress clientAddress;
    private int clientPort;
    private DatagramPacket response;
    private DatagramPacket request;
    private MessageData message;
    /**
     * Create according to the Socket of the given client
     * Thread body
     * @param socket
     */
    public RequestHandler(DatagramSocket socket) throws IOException {
        byte[] recebe = new byte[1000];
        DatagramPacket request = new DatagramPacket(recebe, recebe.length);
        socket.receive(request); //The receive() method blocks until a datagram is received. And the following code sends a DatagramPacket to the client:
        this.command = new String(recebe, 0, recebe.length);
        this.message = new MessageData (command);
        this.socket = socket;
        this.request = request;
    }

    public void run() {

        try{
            String resposta;
            int offset;
            int size;
            switch (message.guessPedido ()){
                case 1:
                    System.out.println("Metadata Request -" + command);
                    resposta = message.getMetadata ();

                    byte[] buffer = resposta.getBytes();

                    clientAddress = request.getAddress();
                    clientPort = request.getPort();

                    //Answers back with metadata
                    response = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
                    socket.send(response);
                    break;
                case 2:
                    System.out.println("GET File request -\n" + command);

                    //get only offset to offset + size bytes
                    byte[] responder = message.getFile();

                    clientAddress = request.getAddress();
                    clientPort = request.getPort();
                    //send bytes requested
                    response = new DatagramPacket(responder, responder.length, clientAddress, clientPort);
                    socket.send(response);

                    System.out.println ("Packets sent");
                    break;
                default:
                    System.out.println("Unavailable request");
                    break;
            }


        }catch(Exception e){
            System.out.println (e);

        }

    }

}

