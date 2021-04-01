import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class HttpGwAZIA {
    public static void main(String[] args) {
        String recepcao = "C:\\Users\\comta\\OneDrive\\Ambiente de Trabalho\\4ano\\CC\\CC2021\\FastFileSrv\\src\\main\\recepção"; // Local onde vão ser armazenados
        boolean running =true;
        String hostname = "localhost";
        int port = 4445;
        String pedido = "GET cc.mp4 \n" + //mensagem a enviar/ pedido
            "TAM 2222\n"+ //supostamente na net dz que um udp tem que levar size mas neste caso nem sei o que é
            "DESTINO 1234\n"+ // vai ser necessário enviar para um determinado servidor disponivel depois
            "PORTA 1234"; //magicamente isto vai trazer uma mensagem do http get etc por agora é trial

        byte[] pedidob;

        try {
        InetAddress address = InetAddress.getByName(hostname);
        DatagramSocket socket = new DatagramSocket();

        while (running) {
            //envia um pedido
            pedidob = pedido.getBytes ();
            DatagramPacket request = new DatagramPacket(pedidob,pedidob.length, address, port);
            socket.send(request);



            // se o pedido por para fazer get
            createFile( recepcao, socket); // Creating the file




            System.out.println("Pós create file");
            System.out.println();
            running = false;
            //Thread.sleep(10000);
        }

    } catch (SocketTimeoutException ex) {
        System.out.println("Timeout error: " + ex.getMessage());
        ex.printStackTrace();
    } catch (IOException ex) {
        System.out.println("Client error: " + ex.getMessage());
        ex.printStackTrace();

    }
}
    public static void createFile (String recepcao,DatagramSocket socket){
        try{
            //recebe resposta com o nome do ficheiro a criar
            byte[] buffer = new byte[512];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);

            String quote = new String(buffer, 0, response.getLength());

            System.out.println("A criar ficheiro na recepcao com o nome "+ quote);
            File f = new File (recepcao + "\\" + quote); // Cria o ficheiro na path indicada
            FileOutputStream outToFile = new FileOutputStream(f); // Criação do stream para receber o file
            System.out.println ("ficheiro criado!!!! <3");
            recepcao(outToFile, socket); // Receber o file
        }catch(Exception ex){
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static void recepcao(FileOutputStream outToFile, DatagramSocket socket) throws IOException {
        System.out.println("A receber file");
        boolean flag; // Flag teste para ver se o file acabou
        int sequenceNumber = 0; // ordem das sequencias
        int foundLast = 0; // ultimo

        while (true) {
            byte[] message = new byte[1024]; // info recebida (armazenar)
            byte[] fileByteArray = new byte[1021]; // para ser escrito no ficheiro

            // receber packets
            DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
            socket.receive(receivedPacket);
            message = receivedPacket.getData(); // get info

            // portas para enviar ACKs
            InetAddress address = receivedPacket.getAddress();
            int port = receivedPacket.getPort();

            // obter o numero da sequencia
            sequenceNumber = ((message[0] & 0xff) << 8) + (message[1] & 0xff);
            // checkae se estamos no fim do file
            flag = (message[2] & 0xff) == 1;

            // se o numero de sequencia for igual ao foundlast +1 supostamente acabou mas não tenho a certeza se isto morre por causa deste if kkkkk
            // recebe data da mensgaem e envia respetivo ack
            if (sequenceNumber == (foundLast + 1)) {

                // definir o last
                foundLast = sequenceNumber;

                // get data da mensgaem
                System.arraycopy(message, 3, fileByteArray, 0, 1021);

                // escrve no ficheiro
                outToFile.write(fileByteArray);
                System.out.println("recebido: numero de sequencia:" + foundLast);

                // send acks
                sendAck(foundLast, socket, address, port);
            } else {
                System.out.println("sequencia errada:  à espera de " + (foundLast + 1) + ", recebido:  " + sequenceNumber + ". RIP");
                // resend ack
                sendAck(foundLast, socket, address, port);
            }
            // morre quando acaba de receber tud0
            if (flag) {
                outToFile.close();
                break;
            }
        }
    }

    private static void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port) throws IOException {
        // send acks
        byte[] ackPacket = new byte[2];
        ackPacket[0] = (byte) (foundLast >> 8);
        ackPacket[1] = (byte) (foundLast);
        // datagrama a aenviar
        DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);
        socket.send(acknowledgement);
        System.out.println("ack enviado: numero de sequencia = " + foundLast);
    }
}
