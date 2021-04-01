import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HTTPRequest {
    public String method, path, version, host;
    public String file;
    public List<String> headers = new ArrayList<>();

    public HTTPRequest(String request) {
        String[] requestsLines = request.split("\n");
        String[] requestLine = requestsLines[0].split(" ");
        method = requestLine[0];
        path = requestLine[1];
        file = getFileName(path);
        version = requestLine[2];
        host = requestsLines[1].split(" ")[1];

        for (int h = 2; h < requestsLines.length; h++) {
            String header = requestsLines[h];
            headers.add(header);
        }
    }

    private String getFileName(String path){
        if ("/".equals(path)) {
            return "index.html";
        }

        return path.substring(1);
    }
}
