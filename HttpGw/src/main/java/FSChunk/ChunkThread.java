package FSChunk;

import Utils.MyPair;
import Utils.SocketPool;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ChunkThread extends Thread {
    private ArrayList<Integer> offsets;
    private String fileName;
    private int fileSize;
    private int packetSize;
    private boolean last;
    private Map<Integer, MyPair<Condition,byte[]>> responseStruct;
    private ReentrantLock arrayLock;
    private SocketPool socketPool;
    private HashMap<InetAddress, ArrayList<Integer>> servers;

    public ChunkThread(ArrayList<Integer> offsets,
                       String fileName,
                       int fileSize,
                       int packetSize,
                       boolean last,
                       Map<Integer, MyPair<Condition,byte[]>> responseStruct,
                       ReentrantLock arrayLock,
                       SocketPool socketPool,
                       HashMap<InetAddress, ArrayList<Integer>> servers) {
        this.offsets = offsets;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.packetSize = packetSize;
        this.last = last;
        this.responseStruct = responseStruct;
        this.arrayLock = arrayLock;
        this.socketPool = socketPool;
        this.servers = servers;
    }

    @Override
    public void run() {
        byte[] fileChunk;
        DatagramSocket socket = socketPool.getSocket();
        FSChunkWorker worker = new FSChunkWorker(socket, fileName, servers);
        // If Thread has the last packet and that last packet isn't the same size as others get size - 1 else get size
        // The different sized packet will always be at the end of the array if it exists
        int equalPackets = last ? offsets.size() - 1 : offsets.size();
        // Capture same length packets
        for(int i = 0; i < equalPackets;i++) {
            try {
                fileChunk = worker.getFile(offsets.get(i) * packetSize,packetSize);
                arrayLock.lock();
                responseStruct.get(offsets.get(i)).setSecond(fileChunk);
                // Wakes up the retriever thread
                responseStruct.get(offsets.get(i)).getFirst().signal();
            } catch (NoSuchFieldException e) {
                System.out.println(e.getMessage());
            } finally {
                arrayLock.unlock();
            }
        }
        // Capture last different length packet, if this thread has one
        if(last) {
            int lastOffset = offsets.get(offsets.size()-1);
            int lastPacketSize = fileSize - lastOffset * packetSize;
            try {
                fileChunk = worker.getFile(lastOffset * packetSize,lastPacketSize);
                arrayLock.lock();
                responseStruct.get(lastOffset).setSecond(fileChunk);
                // Wakes up the retriever thread
                responseStruct.get(lastOffset).getFirst().signal();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } finally {
                arrayLock.unlock();
            }
        }
        socketPool.releaseSocket(socket);
    }
}
