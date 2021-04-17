package FSChunk;

import Utils.MyPair;

import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class DataRetrieverThread extends Thread {
    private byte[] fileContentArray;
    private HashMap<Integer, MyPair<Condition,byte[]>> fileContent;
    private ReentrantLock fileContentLock;
    private int packetSize;
    private int numPackets;

    public DataRetrieverThread(byte[] fileContentArray,
                               HashMap<Integer, MyPair<Condition,byte[]>> fileContent,
                               ReentrantLock fileContentLock,
                               int packetSize,
                               int numPackets) {
        this.fileContentArray = fileContentArray;
        this.fileContent = fileContent;
        this.fileContentLock = fileContentLock;
        this.packetSize = packetSize;
        this.numPackets = numPackets;
    }

    @Override
    public void run() {
        try {
            fileContentLock.lock();
            MyPair<Condition,byte[]> temporaryPair;
            for (int i = 0; i < numPackets; i++) {
                temporaryPair = fileContent.get(i);
                while (temporaryPair.getSecond() == null) {
                    temporaryPair.getFirst().await();
                }
                System.arraycopy(temporaryPair.getSecond(), 0, fileContentArray, i * packetSize, temporaryPair.getSecond().length);
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } finally {
            fileContentLock.unlock();
        }
    }
}
