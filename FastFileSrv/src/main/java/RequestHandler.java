import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RequestHandler implements Runnable{
    private DatagramSocket socket;
    private String quote ;
    private InetAddress clientAddress;
    private int clientPort;
    private DatagramPacket response;
    private DatagramPacket request;
    /**
     * Create according to the Socket of the given client
     * Thread body
     * @param socket
     */
    public RequestHandler(DatagramSocket socket,String quote,DatagramPacket request){
        this.socket = socket;
        this.quote = quote;
        this.request = request;
    }

    public void run() {

        try{
            String resposta;
            int offset;
            int size;
            switch (guessPedido (quote)){
                case 1:
                    System.out.println("Metadata Request -" + quote);
                    resposta = getMetadata (getNomeFicheiro(quote));

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
                    if(!(resposta.contains ("."))){ //if nome doesn't have extention
                        resposta = filelistCheck (resposta);
                    }
                    offset = getOffset(quote);
                    size = getSize(quote);
                    System.out.println ("hello? "+ resposta);

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


        }catch(Exception e){
            System.out.println (e);

        }

    }
    public static byte[] getFile(String filename, int offset, int size) throws IOException {
        // check for missing extention in file name
        if(!(filename.contains ("."))){ //if nome doesn't have extention
            filename = filelistCheck (filename);
        }
        System.out.println ("nome do ficheiro a pegar: "+filename);
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

        //this may happen when there was .. ou ../ in the requested file name
        if(nome.equals ("")) return "EXISTS:false \n";

        // check for missing extention in file name
        if(!(nome.contains ("."))){ //if nome doesn't have extention
            nome = filelistCheck (nome);
        }
        if(nome.equals ("")) return "EXISTS:false \n";
        System.out.println ("nome do ficheiro dos metadados: "+nome);
        Path filePath = Path.of ("src/main/resources/"+nome);

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
        //Regex looks for pattern that must be invalid when looking for file ../ or ..
        Pattern protecter = Pattern.compile ("(\\.\\.\\/ | \\.\\.)");
        Matcher protect = protecter.matcher (udp);
        if(protect.find ()) {
            System.out.println ("DEBUG: Found invalid file name\n!");
            return "";
        }
        String nome = "";
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
        Pattern pattern = Pattern.compile("((GET\\s\\d*\\s\\d*)|(INFO))\\s(\\w|\\s)*(\\.(\\w|\\d)*)?");
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
        Pattern pattern = Pattern.compile("((GET\\s\\d*\\s\\d*)|(INFO))\\s(\\w|\\s)*(\\.(\\w|\\d)*)?");
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

    public static String filelistCheck(String fname)
    {   System.out.println ("nome do file no check files: "+fname);
        String filenameFinal = "";
        int counterFiles = 0;
        File folder = new File("src/main/resources/");
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles)
        {
            if (file.isFile())
            {
                System.out.println (file.getName ());
                if(!(file.getName ().equals (".gitkeep")) && !(file.getName ().equals (".gitignore"))) {
                    String[] filename = file.getName ().split ("\\."); //split filename from it's extension
                    if (filename[0].equals (fname)) { //matching defined filename
                        counterFiles++;
                        filenameFinal = filename[0] + "." + filename[1];
                    }
                }

            }
        }
        if(counterFiles > 1)
            return ""; //means there was found more than one file with "fname" so it's returned nothing
        return filenameFinal;
    }

}

