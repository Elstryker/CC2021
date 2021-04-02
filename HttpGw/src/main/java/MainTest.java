import java.io.FileNotFoundException;
import java.net.SocketException;

public class MainTest {
    public static void main(String[] args) throws SocketException, FileNotFoundException, InterruptedException {
        FSChunk protocol = new FSChunk();
        protocol.start();
        Thread.sleep(3000);
        MyPair<byte[],String> result = protocol.retrieveFile("Quotes.txt");
        String ret = new String(result.getFirst());
        System.out.println(ret);
        System.out.println(result.getSecond());
        while (true);
    }
}
