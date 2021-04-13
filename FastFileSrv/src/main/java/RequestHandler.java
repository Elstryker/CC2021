import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class RequestHandler implements Runnable{
    private DatagramSocket socket;
    private String command ;
    private InetAddress clientAddress;
    private int clientPort;
    private MessageData message;
    private DatagramPacket response;
    private DatagramPacket request;
    /**
     * Create according to the Socket of the given client
     * Thread body
     * @param socket
     */
    public RequestHandler(DatagramSocket socket) throws IOException {
        byte[] receive = new byte[1000];
        DatagramPacket request = new DatagramPacket(receive, receive.length);
        socket.receive(request); //The receive() method blocks until a datagram is received. And the following code sends a DatagramPacket to the client:
        this.socket = socket;
        this.command = new String(receive, 0, receive.length);;
        this.message = new MessageData (command);
        this.request = request;
    }

    public void run() {
        try{
            String responseS;
            switch (message.guessPedido ()){
                case 1:
                    System.out.println("Metadata Request -" + command);
                    responseS = message.getMetadata ();
                    byte[] buffer = responseS.getBytes();

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

