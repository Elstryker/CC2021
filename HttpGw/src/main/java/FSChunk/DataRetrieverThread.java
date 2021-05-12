package FSChunk;

import Utils.MyPair;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class DataRetrieverThread extends Thread {
    private OutputStream clientStream;
    private HashMap<Integer, MyPair<Condition,byte[]>> fileContent;
    private ReentrantLock fileContentLock;
    private int numPackets;

    public DataRetrieverThread(OutputStream clientStream,
                               HashMap<Integer, MyPair<Condition,byte[]>> fileContent,
                               ReentrantLock fileContentLock,
                               int numPackets) {
        this.clientStream = clientStream;
        this.fileContent = fileContent;
        this.fileContentLock = fileContentLock;
        this.numPackets = numPackets;
    }

    @Override
    public void run() {
        try {
            fileContentLock.lock();
            MyPair<Condition,byte[]> conditionContentPair;
            for (int i = 0; i < numPackets; i++) {
                conditionContentPair = fileContent.get(i);
                while (conditionContentPair.getSecond() == null) {
                    conditionContentPair.getFirst().await();
                }

                fileContent.remove(i); // Removes entry from structure to free up space
                byte[] content = conditionContentPair.getSecond();
                byte[] hexSizeBytes = (Integer.toHexString(content.length) + "\n").getBytes();
                byte[] chunk = new byte[hexSizeBytes.length + content.length + "\n".getBytes().length];

                System.arraycopy(hexSizeBytes, 0, chunk, 0, hexSizeBytes.length);
                System.arraycopy(content, 0, chunk, hexSizeBytes.length, content.length);
                System.arraycopy("\n".getBytes(), 0, chunk, hexSizeBytes.length + content.length, "\n".getBytes().length);
                clientStream.write(chunk);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        } finally {
            fileContentLock.unlock();
        }
    }
}
