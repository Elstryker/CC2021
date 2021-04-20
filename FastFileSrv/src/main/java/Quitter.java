import java.io.IOException;
import java.net.*;
import java.util.Scanner;

class Quitter implements Runnable{

    public Quitter(){
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
        } catch (SocketException e) {
            e.printStackTrace ();
        }
    }

    private void sendExitRequest() throws SocketException {
        DatagramSocket exitRequestSocket = new DatagramSocket ();
        byte[] buffer = "quit".getBytes();
        boolean processCompleted = false;

        while(!processCompleted) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"), 54321);
                exitRequestSocket.send(packet);
                System.out.println("Exit request sent");
                byte[] waiter = new byte[1];
                packet = new DatagramPacket(waiter, waiter.length);
                exitRequestSocket.setSoTimeout(1000);
                exitRequestSocket.receive(packet);
                exitRequestSocket.setSoTimeout(0);
                processCompleted = true;
                System.out.println("Exit process complete");
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        exitRequestSocket.close();
    }
}
