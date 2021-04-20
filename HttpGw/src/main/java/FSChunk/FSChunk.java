package FSChunk;

import Utils.MyPair;
import Utils.SocketPool;
import Utils.Timer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FSChunk {
    // List of available servers
    private HashMap<InetAddress,ArrayList<Integer>> servers;
    // Lock for thread managing on servers structure
    private ReentrantLock serversLock;
    // Socket Pool
    private SocketPool socketPool;


    public FSChunk() throws SocketException {
        servers = new HashMap<>();
        serversLock = new ReentrantLock();
        socketPool = new SocketPool(1000);
    }

    public void start() throws SocketException, NoSuchAlgorithmException {
        new Thread(
                new ServerAssociationWorker(servers, serversLock)
        ).start();

        new Thread(
                new ServerExitWorker(servers, serversLock)
        ).start();
    }

    /*
    Info file  :  EXISTS:true,SIZE:500,TYPE:type | EXISTS:false
    Get offset size file
    */

    public MyPair<byte[],String> retrieveFile(String fileName) throws SocketException, FileNotFoundException {
        DatagramSocket socket = socketPool.getSocket();
        // Get Random Server to fetch files
        MyPair<InetAddress,Integer> server = getRandomServer();
        // Get MetaData Information
        FileMetaData fileMetaData = getMetaData(socket, fileName, server.getFirst(), server.getSecond());
        socketPool.releaseSocket(socket);
        // Debugging metaData information
        System.out.println("\n" + fileMetaData.toString() + "\n\n");
        // Get file from server if file exists, else throw exception
        if(fileMetaData.fileExists()) {
            byte[] fileContent = getFile(fileName,fileMetaData.getSize());
            return new MyPair<>(fileContent,fileMetaData.getType());
        }
        else throw new FileNotFoundException("File Not Found");
    }

    private MyPair<InetAddress,Integer> getRandomServer() {
        InetAddress serverAddress;
        ArrayList<Integer> ports;
        int destPort;
        try {
            serversLock.lock();
            // Getting keys of the Map as a list
            List<InetAddress> addressList = new ArrayList<>(servers.keySet());
            // Get a random address from the list
            int randomIP = new Random().nextInt(addressList.size());
            serverAddress = addressList.get(randomIP);
            ports = servers.get(serverAddress);
            // Get a random port from the list
            int randomPort = new Random().nextInt(ports.size());
            destPort = ports.get(randomPort);
        } finally {
            serversLock.unlock();
        }
        return new MyPair<>(serverAddress,destPort);
    }

    private byte[] getFile(String file, int size) {
        // Matrix that assigns a list of offsets to the respective thread
        ArrayList<ArrayList<Integer>> offSetsForThreads = new ArrayList<>();
        int numThreads = (size / (15 * 1024 * 1024)) + 1;
        System.out.println("Number of threads: " + numThreads);
        int packetSize = 1024 * 5;
        // Checking time to download the files
        Timer.start();
        int numEqualLengthPackets = size / packetSize;
        // Boolean to determine if the file needs a last packet that has not the same size and the others
        boolean last = size % packetSize != 0;
        int lastOffset;
        // Structure that maps offset packet to the corresponding bytes of the file
        // It has a condition so the thread collecting the data can sleep on it if the data hasn't been retrieved yet
        HashMap<Integer,MyPair<Condition,byte[]>> fileContent = new HashMap<>();
        ReentrantLock fileContentLock = new ReentrantLock();
        // Check if the last different sized packet is needed and update the lastOffest variable if affirmative
        if(last) {
            lastOffset = numEqualLengthPackets + 1;
        }
        else
            lastOffset = numEqualLengthPackets;
        // Initialization of structures and population of offsets matrix
        initializeOffsetsAndDataStructure(offSetsForThreads,fileContent,fileContentLock,numThreads,numEqualLengthPackets,lastOffset);
        try {
            // Different threads are created and run with the respective list of offsets to retrive from the server
            launchChunkThreads(numThreads,offSetsForThreads,file,size,packetSize,last,fileContent,fileContentLock);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
        // Final file content byte array
        byte[] fileContentArray = new byte[size];
        // Thread that collects the data retrieved from the past threads and order the various chunks
        DataRetrieverThread dataRetrieverThread = new DataRetrieverThread(fileContentArray,fileContent,fileContentLock,packetSize,lastOffset);
        dataRetrieverThread.start();
        try {
            dataRetrieverThread.join();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
        Timer.stop();
        System.out.println(Timer.getTimeString());
        return fileContentArray;
    }

    private void initializeOffsetsAndDataStructure(ArrayList<ArrayList<Integer>> offSetsForThreads,
                                                   HashMap<Integer,MyPair<Condition,byte[]>> fileContent,
                                                   ReentrantLock fileContentLock,
                                                   int numThreads,
                                                   int numEqualLengthPackets,
                                                   int lastOffset) {
        int pointer = 0;
        for(int i = 0; i < numThreads; i++)
            offSetsForThreads.add(new ArrayList<>());
        // Populate offset matrix with the round robin algorithm
        for(int i = 0; i < numEqualLengthPackets; i++) {
            offSetsForThreads.get(pointer).add(i);
            pointer = (pointer + 1) % numThreads;
        }
        // Add the different sized packet at the end if it exists
        if(numEqualLengthPackets != lastOffset)
            offSetsForThreads.get(0).add(lastOffset - 1);
        for(int i = 0; i < lastOffset; i++) {
            MyPair<Condition,byte[]> temporaryPair = new MyPair<>(fileContentLock.newCondition(),null);
            fileContent.put(i,temporaryPair);
        }
    }

    private void launchChunkThreads(int numThreads, ArrayList<ArrayList<Integer>> offSetsForThreads,
                                      String file, int size, int packetSize, boolean last, Map<Integer,
                                      MyPair<Condition,byte[]>> fileContent,
                                      ReentrantLock fileContentLock) throws InterruptedException {
        HashMap<InetAddress,ArrayList<Integer>> servers = getServers();
        for (int i = 0; i < numThreads; i++) {
            ChunkThread thread = new ChunkThread(offSetsForThreads.get(i),
                    file,
                    size,
                    packetSize,
                    i == 0 && last, // The different sized packet will always be put in the first thread, if it exists
                    fileContent,
                    fileContentLock,
                    socketPool,
                    servers
            );
            thread.start();
        }
    }

    private HashMap<InetAddress,ArrayList<Integer>> getServers() {
        // Get a copy of the servers structure for worker
        HashMap<InetAddress,ArrayList<Integer>> result;
        try {
            serversLock.lock();
            result = new HashMap<>();
            ArrayList<Integer> temporaryList;
            for (Map.Entry<InetAddress,ArrayList<Integer>> entry : servers.entrySet()) {
                temporaryList = new ArrayList<>(entry.getValue());
                result.put(entry.getKey(),temporaryList);
            }
        } finally {
            serversLock.unlock();
        }
        return result;
    }

    private FileMetaData getMetaData(DatagramSocket socket, String file, InetAddress destAddress, Integer destPort) {
        FSChunkWorker work = new FSChunkWorker(socket, file, destAddress, destPort);
        FileMetaData metaData = null;
        try {
            metaData = work.getMetaData();
        } catch (NoSuchFieldException e) {
            System.out.println(e.getMessage());
        }
        return metaData;
    }
}
