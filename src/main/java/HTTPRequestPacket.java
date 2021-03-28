import java.util.ArrayList;
import java.util.Arrays;

public class HTTPRequestPacket {
    ArrayList<String> header;
    String type, file, version;
    String content;

    public HTTPRequestPacket(String allContent) {
        String[] tokens = allContent.split("\n");
        String[] firstTokens = tokens[0].split(" ");
        type = firstTokens[0];
        file = firstTokens[1].substring(1);
        version = firstTokens[2];
        header = new ArrayList<>();
        header.addAll(Arrays.asList(tokens));
        header.remove(0);
        this.content = "";
    }
}
