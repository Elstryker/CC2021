import java.io.IOException;
import java.net.*;
import java.util.Scanner;

class Quitter implements Runnable{

    private final DatagramSocket socketInUse;
    private final int socketPort;

    public Quitter(DatagramSocket socketInUse, int socketPort){
         this.socketInUse = socketInUse;
         this.socketPort = socketPort;
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
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"), 54321);
                exitRequestSocket.send(packet);
                byte[] waiter = new byte[1];
                packet = new DatagramPacket(waiter, waiter.length);
                exitRequestSocket.setSoTimeout(3000);
                exitRequestSocket.receive(packet);
                exitRequestSocket.setSoTimeout(0);
                processCompleted = true;
                System.out.println("Exit process complete");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        exitRequestSocket.close();
    }
}
