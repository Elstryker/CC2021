package FSChunk;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class ChunkThread extends Thread {
    private ArrayList<Integer> offsets;
    private String fileName;
    private int fileSize;
    private int packetSize;
    private boolean last;
    private Map<Integer,byte[]> finalArray;
    private ReentrantLock arrayLock;
    private DatagramSocket socket;
    private HashMap<InetAddress, ArrayList<Integer>> servers;

    public ChunkThread(ArrayList<Integer> offsets,
                       String fileName,
                       int fileSize,
                       int packetSize,
                       boolean last,
                       Map<Integer, byte[]> finalArray,
                       ReentrantLock arrayLock,
                       DatagramSocket socket,
                       HashMap<InetAddress, ArrayList<Integer>> servers) {
        this.offsets = offsets;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.packetSize = packetSize;
        this.last = last;
        this.finalArray = finalArray;
        this.arrayLock = arrayLock;
        this.socket = socket;
        this.servers = servers;
    }

    @Override
    public void run() {
        byte[] fileChunk;
        FSChunkWorker worker = new FSChunkWorker(socket, fileName, servers);
        int equalPackets = last ? offsets.size() - 1 : offsets.size();
        for(int i = 0; i < equalPackets;i++) {
            try {
                fileChunk = worker.getFile(offsets.get(i) * packetSize,packetSize);
                arrayLock.lock();
                finalArray.put(offsets.get(i),fileChunk);
            } catch (NoSuchFieldException e) {
                System.out.println(e.getMessage());
            } finally {
                arrayLock.unlock();
            }
        }
        if(last) {
            int lastOffset = offsets.get(offsets.size()-1);
            int lastPacketSize = fileSize - lastOffset * packetSize;
            try {
                fileChunk = worker.getFile(lastOffset * packetSize,lastPacketSize);
                arrayLock.lock();
                finalArray.put(lastOffset,fileChunk);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } finally {
                arrayLock.unlock();
            }
        }
    }
}
