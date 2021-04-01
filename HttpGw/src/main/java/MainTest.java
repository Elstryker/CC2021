import java.io.FileNotFoundException;
import java.net.SocketException;

public class MainTest {
    public static void main(String[] args) throws SocketException, FileNotFoundException {
        FSChunk protocol = new FSChunk();
        System.out.println(new String(protocol.retrieveFile("Batatas")));
    }
}
