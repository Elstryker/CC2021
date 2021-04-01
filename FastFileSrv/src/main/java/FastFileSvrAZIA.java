import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FastFileSvrAZIA {
    private final DatagramSocket socket;

    public FastFileSvrAZIA(int port) throws SocketException, SocketException {
        socket = new DatagramSocket(4445); //  The following code creates a UDP server listening on port 17
    }


    public static void main(String[] args) {

        int port = 18;

        try {
            FastFileSvrAZIA server = new FastFileSvrAZIA (port);
            server.service();
        } catch (SocketException ex) {
            System.out.println("Socket error: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    private void service() throws IOException {
        while (true) {
            String resposta = "";
            byte[] pedidob = new byte[1024];
            DatagramPacket request = new DatagramPacket(pedidob,pedidob.length);
            socket.receive(request); //The receive() method blocks until a datagram is received. And the following code sends a DatagramPacket to the client:
            String quote = new String(pedidob, 0, pedidob.length);
            //System.out.println(quote); // pedido feito pelo httpgw

            switch (guessPedido (quote)){
                case 1:
                    System.out.println("Pedido de Metadados -\n" + quote+"\n"); //string a tratar
                    resposta = getMetadata (getNomeFicheiro (quote));
                    //enviaMetadados(receive,ds,address);
                    break;
                case 2:
                    System.out.println("Pedido de Transferencia de Ficheiro -\n" + quote); //string a tratar
                    resposta = getNomeFicheiro (quote);
                    //mens o intelIJ tem que morrer nunca reconhece paths de jeito
                    File f = new File("C:\\Users\\comta\\OneDrive\\Ambiente de Trabalho\\4ano\\CC\\CC2021\\FastFileSrv\\src\\main\\resources\\"+ resposta);
                    byte[] buffer = resposta.getBytes();
                    InetAddress clientAddress = request.getAddress(); // get address de quem enviou o pedido
                    int clientPort = request.getPort();
                    //responde ao cliente o nome do ficheiro a criar
                    DatagramPacket response = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
                    socket.send(response);


                    byte[] fileByteArray = getBytesArray(f);
                    System.out.println ("Enviou o nome do file para o httpGw para a sua criação");
                    sendFile(socket, fileByteArray, clientAddress, clientPort); // actually enviar
                    resposta = "por fazer";
                    //trial fs = new trial ();
                    //fs.ready(port, host,getNomeFicheiro (udpPedido));
                    break;
                case 0:
                    System.out.println("Pedido de Autenticação -\n" + quote); //Noa sei se isto é preciso
                    resposta =" por fzaer";
                    break;
                default:
                    System.out.println("Pedido sem tratamento");
                    resposta = "caiu no default";
                    break;
            }


            byte[] buffer = resposta.getBytes();

            InetAddress clientAddress = request.getAddress(); // get address de quem enviou o pedido
            int clientPort = request.getPort();

            //responde ao cliente
            DatagramPacket response = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
            socket.send(response);
        }
    }



    private int guessPedido(String udp){
        Pattern pattern = Pattern.compile("INFO");
        Matcher matcher = pattern.matcher(udp);
        if (matcher.find()) //pedido do tipo get de Metadados
        {
            return 1;
        }
        pattern = Pattern.compile("GET");
        matcher = pattern.matcher(udp);
        if (matcher.find()) //pedido do tipo get de ficheiro
        {
            return 2;
        }
        pattern = Pattern.compile("AUTH");
        matcher = pattern.matcher(udp);
        if (matcher.find()) //pedido de autenticação
        {
            return 0;
        }
        return -1;
    }

    public static String getMetadata(String nome) throws IOException {
        System.out.println (nome);
        Path file = Path.of ("C:\\Users\\comta\\OneDrive\\Ambiente de Trabalho\\4ano\\CC\\CC2021\\FastFileSrv\\src\\main\\resources\\"+nome);
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
        //METADADOS: //nao sei se estes sao os melhoras mas depois vê-se
        Long tamanho = attr.size(); // tamanho
        String lastModified = attr.lastModifiedTime().toString ();
        String created = attr.creationTime().toString ();
        String tipo = nome.split ("\\.")[1];

        String meta = "Metadados do Ficheiro:\n"+
                "Nome: "+nome+
                "\nTipo: "+tipo+
                "\nTamanho: "+tamanho+
                "bytes\nDataCriacao: "+created+
                "\nDataUltimaModificacao: "+lastModified+"\n";
        return meta;
    }

    public static String getNomeFicheiro(String udp){
        String nome = ""; // depois a default vai ser 80 mas wharetver
        Pattern pattern = Pattern.compile("GET\\s+(\\d|\\w|\\_)*.(\\w|\\d)*");
        Matcher matcher = pattern.matcher(udp);
        if (matcher.find())
        {
            String r = matcher.group ();
            String[] parts = r.split(" ");
            nome = parts[1];
        }
        return nome;
    }

    private static byte[] getBytesArray(File file) {
        FileInputStream fis = null;
        // Creating a byte array using the length of the file
        // file.length returns long which is cast to int
        byte[] bArray = new byte[(int) file.length()];
        try {
            fis = new FileInputStream(file);
            fis.read(bArray);
            fis.close();

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
        }
        return bArray;
    }

    private void sendFile(DatagramSocket socket, byte[] fileByteArray, InetAddress address, int port) throws IOException {
        System.out.println("A enviar Ficheiro");
        int ordem = 0; // ordem da sequencia
        boolean flag; // flag para assinalar quando chegamos ao fim da sequencia
        int ackSequence = 0; // recebes acks para ver se não está a morrer nada

        for (int i = 0; i < fileByteArray.length; i = i + 1021) {
            ordem += 1;

            // Create message
            byte[] message = new byte[1024]; // First two bytes of the data are for control (datagram integrity and order)
            message[0] = (byte) (ordem >> 8);
            message[1] = (byte) (ordem);

            if ((i + 1021) >= fileByteArray.length) { // Acabou o file?
                flag = true;
                message[2] = (byte) (1); // eof -> last to send
            } else {
                flag = false;
                message[2] = (byte) (0); // not eof -> continuar envio de datagrams
            }

            if (!flag) {
                System.arraycopy(fileByteArray, i, message, 3, 1021);
            } else { // If it is the last datagram
                System.arraycopy(fileByteArray, i, message, 3, fileByteArray.length - i);
            }

            DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, port); // data a enviar
            socket.send(sendPacket); // envia data
            System.out.println("Enviou: numero de sequencia = " + ordem);

            //parte para testar se o datagrama foi recebido
            boolean ackRec;

            while (true) {
                byte[] ack = new byte[2]; // Criar sempre um porque isto morre e eu nao sei pq rip
                DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

                try {
                    socket.setSoTimeout(50); // Não sei se deve haver timeout aqui mas pelo sim pelo não é melhor
                    socket.receive(ackpack);
                    ackSequence = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff); // toma em conta a ordem de sequencia
                    ackRec = true; //ack recebido yey
                } catch (SocketTimeoutException e) {
                    System.out.println("TIMEOUT");
                    ackRec = false; // ack nao recebido
                }

                // confirma que o packet foi bem recebido para poder enviar o proximo datagrama
                if ((ackSequence == ordem) && (ackRec)) {
                    System.out.println("Ack recebido: num sequencia = " + ackSequence);
                    break;
                } // pakcage morreu vai reenviar
                else {
                    socket.send(sendPacket);
                    System.out.println("reenviar: num sequencia = " + ordem);
                }
            }
        }
    }
}
