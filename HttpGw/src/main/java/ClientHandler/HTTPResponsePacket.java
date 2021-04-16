package ClientHandler;

import java.io.OutputStream;
import java.util.ArrayList;

public class HTTPResponsePacket {
    ArrayList<String> header = null;
    int maxSize = 16 * 1024, length = 0;
    byte[] content = null;
    String version;
    OutputStream toSend;

    public HTTPResponsePacket(OutputStream socketOutputStream, String ver) {
        toSend = socketOutputStream;
        version = ver;
    }

    public void notFound() {
        header = new ArrayList<>();
        header.add(version + "404 Not Found");
    }

    public void validatePacket() {
        header = new ArrayList<>();
        header.add(version + "200 OK");
    }

    public void addHeaderInfo(String info) {
        if (header == null) {
            header = new ArrayList<>();
        }
        header.add(info);
    }

    public void addContent(byte[] info, int n) {
        if (content == null) {
            content = new byte[maxSize];
            length = 0;
        }
        for (int i = length, j = 0; i < length + n; i++, j++) {
            content[i] = info[j];
        }
    }

    public void setContent(byte[] content) {
        this.content = new byte[content.length];
        for (int i = 0; i < content.length; i++)
            this.content[i] = content[i];
    }
}
