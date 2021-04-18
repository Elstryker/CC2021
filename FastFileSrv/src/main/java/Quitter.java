import java.io.IOException;
import java.net.*;
import java.util.Scanner;

class Quitter implements Runnable{

    public Quitter(){
    }
    public void run() {
        Scanner input = new Scanner (System.in);
        String request = input.nextLine();
        System.out.println (request);
        while(!request.equals ("quit")){
            request = input.nextLine();
            System.out.println (request);
        }

        try {
            getQuitterPacket (request);
        } catch (SocketException e) {
            e.printStackTrace ();
        }

        System.out.println("GoodBye...");
        System.exit (0);
    }

    private void getQuitterPacket(String request) throws SocketException {
        DatagramSocket quit = new DatagramSocket ();
        byte[] buffer = request.getBytes();
        boolean quitDone = false;
        while(!quitDone) {
            try {
                quit.setSoTimeout(1000);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"), 12345);
                quit.send(packet);
                System.out.println("Enviei Pacote quit");
                byte[] waiter= new byte[1];
                packet = new DatagramPacket(waiter, waiter.length);
                System.out.println("Espero Resposta");
                quit.receive(packet);
                quitDone = true;
                quit.setSoTimeout(0);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        quit.close();
    }
}
