import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
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

    public FastFileSrv(int port) throws SocketException {
        socket = new DatagramSocket(4445); //  The following code creates a UDP server listening on port 17

    }

    public static void main(String[] args) throws IOException {

        int port = 18;
            FastFileSrv server = new FastFileSrv (port);
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


            String quote = new String(recebe, 0, recebe.length); //pedido em string

            switch (guessPedido (quote)){
                case 1:
                    System.out.println("Pedido de Metadados -\n" + quote+"\n"); //string a tratar
                    resposta = getMetadata (getNomeFicheiro (quote));
                    byte[] buffer = resposta.getBytes();

                    clientAddress = request.getAddress(); // get address de quem enviou o pedido
                    clientPort = request.getPort();

                    //responde ao cliente
                    response = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
                    socket.send(response);
                    break;
                case 2:
                    System.out.println("Pedido de Transferencia de Ficheiro -\n" + quote); //string a tratar
                    resposta = getNomeFicheiro (quote);
                    offset = getOffset(quote);
                    size = getSize(quote);

                    //mens o intelIJ tem que morrer nunca reconhece paths de jeito
                    File f = new File("src\\main\\resources\\"+ resposta);
                    //byte[] fileByteArray = getBytesArray(f);
                    byte[] fileByteArray = Files.readAllBytes(f.toPath ());
                    byte[] responder = new byte[size];
                    //get from offset + size
                    for(int i=offset, j = 0; j<size && i<fileByteArray.length ;i++,j++){
                        responder[j] = fileByteArray[i];
                    }

                    clientAddress = request.getAddress(); // get address de quem enviou o pedido
                    clientPort = request.getPort();
                    //responde ao cliente o nome do ficheiro a criar
                    response = new DatagramPacket(responder, responder.length, clientAddress, clientPort);
                    socket.send(response);

                    System.out.println ("Enviou packets");
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

            InetAddress clientAddress = request.getAddress();
            int clientPort = request.getPort();

            DatagramPacket response = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
            socket.send(response);
        }
    }

    public static String getMetadata(String nome)  {
        String meta;
        Path file = Path.of ("C:\\Users\\comta\\OneDrive\\Ambiente de Trabalho\\4ano\\CC\\CC2021\\FastFileSrv\\src\\main\\resources\\"+nome);
        BasicFileAttributes attr = null;
        try {
            attr = Files.readAttributes(file, BasicFileAttributes.class);
            Long tamanho = attr.size(); // tamanho
            meta = "EXISTS: true "+
                    "SIZE: "+tamanho+"\n";
        } catch (IOException e) {
            meta = "EXISTS: false \n";
        }
        return meta;
    }

    public static String getNomeFicheiro(String udp){
        String nome = ""; // depois a default vai ser 80 mas wharetver
        Pattern pattern = Pattern.compile("((GET\\s\\d*\\s\\d*)|(INFO))\\s(\\w|\\s)*\\.(\\w|\\d)*");
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
        pattern = Pattern.compile("AUTH");
        matcher = pattern.matcher(udp);
        if (matcher.find()) //pedido de autenticação
        {
            return 0;
        }
        return -1;
    }

}
