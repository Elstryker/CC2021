import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FastFileSrv {
    private final DatagramSocket socket;
    private InetAddress clientAddress;
    private int clientPort;
    private DatagramPacket response;

    public FastFileSrv() throws SocketException {
        socket = new DatagramSocket();
        byte[] aut = new byte[1];
        boolean sent = false;
        while(!sent) {
            try {
                socket.setSoTimeout(1000);
                DatagramPacket packet = new DatagramPacket(aut, aut.length, InetAddress.getByName("localhost"), 12345);
                socket.send(packet);
                System.out.println("Enviei Pacote autenticacao");
                packet = new DatagramPacket(aut, aut.length);
                System.out.println("Espero Resposta");
                socket.receive(packet);
                sent = true;
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        System.out.println("Autenticado!");
        socket.setSoTimeout(0);
    }

    public static void main(String[] args) throws IOException {

        FastFileSrv server = new FastFileSrv();
        server.service();


    }

    private void service() throws IOException {

        while (true) {
            byte[] recebe = new byte[1000];
            String resposta;
            int offset;
            int size;
            DatagramPacket request = new DatagramPacket(recebe, recebe.length);
            socket.receive(request); //The receive() method blocks until a datagram is received. And the following code sends a DatagramPacket to the client:


            String quote = new String(recebe, 0, recebe.length);

            switch (guessPedido (quote)){
                case 1:
                    System.out.println("Metadata Request -" + quote);
                    String nome = getNomeFicheiro(quote);
                    System.out.println("Nome do ficheiro: " + nome);
                    resposta = getMetadata (nome);

                    byte[] buffer = resposta.getBytes();

                    clientAddress = request.getAddress();
                    clientPort = request.getPort();

                    //Answers back with metadata
                    response = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
                    socket.send(response);
                    break;
                case 2:
                    System.out.println("GET File request -\n" + quote);
                    resposta = getNomeFicheiro (quote);
                    offset = getOffset(quote);
                    size = getSize(quote);
                    System.out.println ("hello? :"+ resposta);

                    //get only offset to offset + size bytes
                    byte[] responder = getFile(resposta,offset,size);

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
        }
    }

    public static byte[] getFile(String filename, int offset, int size) throws IOException {
        File f = new File("src/main/resources/"+ filename);
        byte[] responseBytes = new byte[size];
        RandomAccessFile raf = new RandomAccessFile(f, "rw");

        // read from offset to offset + size
        raf.seek(offset);
        raf.read(responseBytes, 0, size);
        raf.close();
        return responseBytes;
    }

    public static String getMetadata(String nome)  {
        Path filePath = Path.of ("src/main/resources/"+nome);
        System.out.println(filePath);
        if(Files.exists(filePath)) {
            try {
                BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
                long size = attr.size();
                String type = Files.probeContentType(filePath);
                return "EXISTS:true" +
                        ",SIZE:" + size +
                        ",TYPE:" + type + "\n";
            } catch (IOException e) {
                return "EXISTS:false \n";
            }
        } else return "EXISTS:false \n";
    }

    public static String getNomeFicheiro(String udp){
        String nome = ""; // depois a default vai ser 80 mas wharetver
        Pattern pattern = Pattern.compile("((GET\\s\\d*\\s\\d*)|(INFO))\\s(\\w|\\s)*(\\.(\\w|\\d)*)?");
        Matcher matcher = pattern.matcher(udp);
        if (matcher.find())
        {   String r = matcher.group ();
            String[] parts = r.split(" ");
            try {
                nome = parts[3];
            } catch(ArrayIndexOutOfBoundsException a) {
                nome = parts[1];
            }
        }
        return nome;
    }

    public static int getOffset(String udp){
        int nome =0; // depois a default vai ser 80 mas wharetver
        Pattern pattern = Pattern.compile("((GET\\s\\d*\\s\\d*)|(INFO))\\s(\\w|\\s)*\\.(\\w|\\d)*");
        Matcher matcher = pattern.matcher(udp);
        if (matcher.find())
        {
            String r = matcher.group ();
            String[] parts = r.split(" ");
            nome = Integer.parseInt (parts[1]);
        }
        return nome;
    }

    public static int getSize(String udp){
        int nome = 0; // depois a default vai ser 80 mas wharetver
        Pattern pattern = Pattern.compile("((GET\\s\\d*\\s\\d*)|(INFO))\\s(\\w|\\s)*\\.(\\w|\\d)*");
        Matcher matcher = pattern.matcher(udp);
        if (matcher.find())
        {
            String r = matcher.group ();
            String[] parts = r.split(" ");
            nome = Integer.parseInt (parts[2]);
        }
        return nome;
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
        return -1;
    }

}
