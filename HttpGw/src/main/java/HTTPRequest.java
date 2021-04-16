import java.util.*;

public class HTTPRequest {
    public String method, path, version, host;
    public String file;
    public Map<String, String> headers = new HashMap<>();
    public String wholeRequest;
    public boolean persistent;

    public HTTPRequest(String request) {
        wholeRequest = request;
        String[] requestsLines = request.split("\n");
        String[] requestLine = requestsLines[0].split(" ");
        method = requestLine[0];
        path = requestLine[1];
        file = getFileName(path);
        version = requestLine[2];
        host = requestsLines[1].split(" ")[1];

        for (int h = 2; h < requestsLines.length; h++) {
            String header = requestsLines[h];
            String[] key_value = header.split(": ");
            headers.put(key_value[0], key_value[1]);
        }

        if (headers.containsKey("Connection")){
            persistent = headers.get("Connection").equalsIgnoreCase("keep-alive");
        }else{
            persistent = false;
        }
    }

    private String getFileName(String path){
        if ("/".equals(path)) {
            return "index.html";
        }

        return path.substring(1);
    }
}
