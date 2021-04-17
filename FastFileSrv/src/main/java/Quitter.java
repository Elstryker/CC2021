import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

class Quitter implements Runnable{
    private DatagramSocket socket;
    private DatagramPacket response;

    public Quitter(DatagramSocket socket){
        this.socket = socket;
    }
    public void run() {
        Scanner ender = new Scanner (System.in);
        String request = ender.nextLine();
        System.out.println (request);
        while(!request.equals ("quit")){
            request = ender.nextLine();
            System.out.println (request);
        }
        byte[] buffer = request.getBytes();

        try {
            response= new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"), 12345);
        } catch (UnknownHostException e) {
            e.printStackTrace ();
        }

        try {
            socket.send(response);
            System.out.println("GoodBye...");
            System.exit (0);
        } catch (IOException e) {
            e.printStackTrace ();
        }
    }
}
