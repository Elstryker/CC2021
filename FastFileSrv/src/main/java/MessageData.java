import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageData {
    private String command;
    private int offset;
    private int size;
    private String fileName;

    public MessageData(String command){
        this.command = command; // to be used to get offset and size
        fileName = getNomeFicheiro (); //filename with or withou extention

        if(!(fileName.contains ("."))){ //if nome doesn't have extention
            fileName = filelistCheck (fileName); //get fileName with extention;
        }
    }
    public synchronized byte[] getFile() throws IOException {
        getOffset ();
        getSize ();
        System.out.println ("nome do ficheiro a pegar: "+fileName);
        File f = new File("src/main/resources/"+ fileName);
        byte[] responseBytes = new byte[size];
        RandomAccessFile raf = new RandomAccessFile(f, "rw");

        // read from offset to offset + size
        raf.seek(offset);
        raf.read(responseBytes, 0, size);
        raf.close();
        return responseBytes;
    }

    public synchronized String getMetadata()  {
        //this may happen when there was .. ou ../ in the requested file name
        if(fileName.equals ("")) return "EXISTS:false \n";

        System.out.println ("nome do ficheiro dos metadados: "+fileName);
        Path filePath = Path.of ("src/main/resources/"+fileName);

        if(Files.exists(filePath)) {
            try {
                BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
                long sizeAttr = attr.size();
                String type = Files.probeContentType(filePath);

                return "EXISTS:true" +
                        ",SIZE:" + sizeAttr +
                        ",TYPE:" + type + "\n";
            } catch (IOException e) {
                return "EXISTS:false \n";
            }
        } else return "EXISTS:false \n";
    }

    public  String getNomeFicheiro(){
        //Regex looks for pattern that must be invalid when looking for file ../ or ..
        Pattern protecter = Pattern.compile ("(\\.\\.(\\/)?)");
        Matcher protect = protecter.matcher (command);
        if(protect.find ()) {
            System.out.println ("DEBUG: Found invalid file name\n!");
            return "";
        }
        String nome = "";
        Pattern pattern = Pattern.compile("((GET\\s\\d*\\s\\d*)|(INFO))\\s(\\w|\\s)*(\\.(\\w|\\d)*)?");
        Matcher matcher = pattern.matcher(command);
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

    public  void getOffset(){
        int nome =0; // depois a default vai ser 80 mas wharetver
        Pattern pattern = Pattern.compile("((GET\\s\\d*\\s\\d*)|(INFO))\\s(\\w|\\s)*(\\.(\\w|\\d)*)?");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find())
        {
            String r = matcher.group ();
            String[] parts = r.split(" ");
            nome = Integer.parseInt (parts[1]);
        }
        offset = nome;
    }

    public  void getSize(){
        int nome = 0; // depois a default vai ser 80 mas wharetver
        Pattern pattern = Pattern.compile("((GET\\s\\d*\\s\\d*)|(INFO))\\s(\\w|\\s)*(\\.(\\w|\\d)*)?");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find())
        {
            String r = matcher.group ();
            String[] parts = r.split(" ");
            nome = Integer.parseInt (parts[2]);
        }
        size = nome;
    }

    public int guessPedido(){
        Pattern pattern = Pattern.compile("INFO");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) //request get metadata
        {
            return 1;
        }
        pattern = Pattern.compile("GET");
        matcher = pattern.matcher(command);
        if (matcher.find()) //request get file
        {
            return 2;
        }
        return -1;
    }

    public  String filelistCheck(String fname)
    {   System.out.println ("nome do file no check files: "+fname);
        String filenameFinal = "";
        int counterFiles = 0;
        File folder = new File("src/main/resources/");
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles)
        {
            if (file.isFile())
            {
                //System.out.println (file.getName ());
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
