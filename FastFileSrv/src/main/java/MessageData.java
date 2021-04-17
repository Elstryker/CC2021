import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageData {
    private final String type;
    private int offset;
    private int size;
    private String fileName;

    public MessageData(String command){
        String[] tokens = command.trim().split(" ");
        type = tokens[0];
        fileName = "";
        if(type.equals("INFO")) {
            fileName = tokens[1];
        }
        else {
            offset = Integer.parseInt(tokens[1]);
            size = Integer.parseInt(tokens[2]);
            fileName = tokens[3];
            // Filtering invalid positions or names
            if(fileName.contains("../") || fileName.contains(".."))
                fileName = "";
        }
        if (!(fileName.contains("."))) //if nome doesn't have extention
            fileName = filelistCheck(fileName); //get fileName with extention;
    }
    public byte[] getFile() throws IOException {
        File f = new File("src/main/resources/"+ fileName);
        byte[] responseBytes = new byte[size];
        RandomAccessFile raf = new RandomAccessFile(f, "rw");

        // read from offset to offset + size
        raf.seek(offset);
        raf.read(responseBytes, 0, size);
        raf.close();
        return responseBytes;
    }

    public String getMetadata()  {
        //this may happen when there was .. ou ../ in the requested file name
        if(fileName.equals ("")) return "EXISTS:false \n";

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

    public String getType() {
        return type;
    }

    public String filelistCheck(String fname)
    {
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