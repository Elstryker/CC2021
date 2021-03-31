public class MainTest {
    public static void main(String[] args) {
        FSChunk protocol = new FSChunk();
        System.out.println(new String(protocol.run("Batatas")));
    }
}
