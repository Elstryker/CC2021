import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class sender {

    public static void main(String[] args) {

        String hostname = "localhost";
        int port = 4445;
        //String pedido = "INFO sebenta.pdf";
        String pedido = "GET 5 5 sebenta.pdf ";
        try {
            InetAddress address = InetAddress.getByName(hostname);
            DatagramSocket socket = new DatagramSocket();

            while (true) {
                byte[] ped = pedido.getBytes ();
                DatagramPacket req = new DatagramPacket(ped, ped.length, address, port);
                socket.send(req);

                byte[] buffer = new byte[1000]; //depois isto vem que ser o tamanho suposto a receber não?
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                socket.receive(response);

                String quote = new String(buffer, 0, response.getLength());

                System.out.println(quote+" tamanho disto: "+response.getLength ());
                //System.out.println();

                //Thread.sleep(10000);
                break;
            }

        } catch (SocketTimeoutException ex) {
            System.out.println("Timeout error: " + ex.getMessage());
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Client error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
