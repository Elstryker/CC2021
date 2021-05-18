import java.io.IOException;
import java.net.*;
import java.util.Scanner;

class Quitter implements Runnable{

    // Port to which the main socket is associated to on the httpGw side
    private final int socketPort;
    private final String listener;

    public Quitter(int socketPort,String listener){
         this.socketPort = socketPort;
         this.listener = listener;
    }
    public void run() {
        Scanner input = new Scanner(System.in);
        String request = input.nextLine();
        System.out.println (request);
        while(!request.equals("quit")){
            request = input.nextLine();
        }

        try {
            sendExitRequest();
            System.exit(0);
        } catch (SocketException e) {
            e.printStackTrace ();
        }
    }

    // A new sucket must be used to send the exit request because the main socket is waiting for requests and can't be used
    private void sendExitRequest() throws SocketException {
        DatagramSocket exitRequestSocket = new DatagramSocket();

        byte[] buffer = String.valueOf(socketPort).getBytes();
        boolean processCompleted = false;

        while(!processCompleted) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(listener), 54321);
                exitRequestSocket.send(packet);
                byte[] waiter = new byte[1];
                packet = new DatagramPacket(waiter, waiter.length);
                exitRequestSocket.setSoTimeout(2000);
                exitRequestSocket.receive(packet);
                exitRequestSocket.setSoTimeout(0);
                processCompleted = true;
                System.out.println("Exit process complete");
            } catch (IOException e) {
                exitRequestSocket.setSoTimeout(0);
                e.printStackTrace();
            }
        }
        exitRequestSocket.close();
    }
}
