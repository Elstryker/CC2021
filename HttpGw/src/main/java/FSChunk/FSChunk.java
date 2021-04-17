package FSChunk;

import Utils.MyPair;
import Utils.SocketPool;
import Utils.Timer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FSChunk {
    // List of available servers
    private HashMap<InetAddress,ArrayList<Integer>> servers;
    // Lock for thread managing on servers structure
    private ReentrantLock serversLock;
    // Socket for accepter thread
    private DatagramSocket accepterSocket;
    // Socket Pool
    private SocketPool socketPool;


    public FSChunk() throws SocketException {
        servers = new HashMap<>();
        accepterSocket = new DatagramSocket(12345);
        serversLock = new ReentrantLock();
        socketPool = new SocketPool(1000);
    }

    public void start() {
        new Thread(this::accepterWorkFlow).start();
    }

    private void accepterWorkFlow() {
        while(true) {
            try {
                byte[] content = new byte[100];
                DatagramPacket packet = new DatagramPacket(content, content.length);
                accepterSocket.setSoTimeout(0);
                // Aceita novo pedido
                accepterSocket.receive(packet);
                String request = new String(packet.getData(),0, packet.getLength());
                if(request.equals("Quit")) {
                    deleteServer(packet.getAddress(), packet.getPort());
                    // Confirmação por linha de comando
                    System.out.printf("Deleted server from Address: %s, from Port: %s%n\n",packet.getAddress(),packet.getPort());
                }
                else {
                    saveServer(packet.getAddress(), packet.getPort());
                    // Confirmação por linha de comando
                    System.out.printf("Accepted server from Address: %s, from Port: %s%n\n",packet.getAddress(),packet.getPort());
                }
                packet = new DatagramPacket(content, content.length, InetAddress.getByName("localhost"), packet.getPort());
                accepterSocket.send(packet);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void saveServer(InetAddress address, int port) {
        try {
            serversLock.lock();
            // Save the new available server
            ArrayList<Integer> ports = servers.get(address);
            if (ports != null)
                ports.add(port);
            else {
                ports = new ArrayList<>();
                ports.add(port);
                servers.put(address,ports);
            }
        } finally {
            serversLock.unlock();
        }
    }

    private void deleteServer(InetAddress address, int port) {
        try {
            serversLock.lock();
            // Remove server form the structure
            ArrayList<Integer> ports = servers.get(address);
            if(ports.size() > 1) {
                ports.remove((Integer) port);
            }
            else
                servers.remove(address);
        } finally {
            serversLock.unlock();
        }
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

    /*
    private byte[] getFile(String file, int size) {
        byte[] fileContent = new byte[size];
        try {
            int offset = 0, packetLength = 1024, fileContentSize = 0;
            int packets = size / packetLength;
            DatagramSocket socket;
            // Get socket from pool
            socket = socketPool.getSocket();
            try {
                HashMap<InetAddress, ArrayList<Integer>> servers = getServers();
                // Initiate worker
                FSChunkWorker worker = new FSChunkWorker(socket, file, servers);
                byte[] fileChunk;
                // Get number of packets that has the length equal of packetLength
                for (int i = 0; i < packets; i++, offset += packetLength) {
                    fileChunk = worker.getFile(offset, packetLength);
                    System.arraycopy(fileChunk, 0, fileContent, fileContentSize, packetLength);
                    fileContentSize += packetLength;
                }
                // Get Last packet that has not the same size of packetLength
                if ((size % packetLength) != 0) {
                    fileChunk = worker.getFile(offset, size - offset);
                    System.arraycopy(fileChunk, 0, fileContent, fileContentSize, size - offset);
                }
            } finally {
                // Release socket from pool
                socketPool.releaseSocket(socket);
            }
        } catch (NoSuchFieldException e) {
            System.out.println(e.getMessage());
        }
        return fileContent;
    }
     */

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
        boolean last = false;
        // We presume that the last packet has the same length as the remaining ones
        int lastOffset = numEqualLengthPackets;
        // Structure that maps offset packet to the corresponding bytes of the file
        // It has a condition so the thread collecting the data can sleep on it if the data hasn't been retrieved yet
        HashMap<Integer,MyPair<Condition,byte[]>> fileContent = new HashMap<>();
        ReentrantLock fileContentLock = new ReentrantLock();
        // Check if the last different sized packet is needed and update the lastOffest variable if affirmative
        if(size % packetSize != 0) {
            last = true;
            lastOffset = numEqualLengthPackets + 1;
        }
        // Initialization of structures and population of offsets matrix
        initializeOffSetsAndDataStructure(offSetsForThreads,fileContent,fileContentLock,numThreads,numEqualLengthPackets,lastOffset);
        try {
            // Different threads are created and run with the respective list of offsets to retrive from the server
            threadCreationAndRun(numThreads,offSetsForThreads,file,size,packetSize,last,fileContent,fileContentLock);
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

    private void initializeOffSetsAndDataStructure(ArrayList<ArrayList<Integer>> offSetsForThreads,
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
        // Add the different sized packet at last if it exists
        if(numEqualLengthPackets != lastOffset)
            offSetsForThreads.get(0).add(lastOffset - 1);
        MyPair<Condition,byte[]> temporaryPair;
        for(int i = 0; i < lastOffset; i++) {
            temporaryPair = new MyPair<>(fileContentLock.newCondition(),null);
            fileContent.put(i,temporaryPair);
        }
    }

    private void threadCreationAndRun(int numThreads, ArrayList<ArrayList<Integer>> offSetsForThreads,
                                      String file, int size, int packetSize, boolean last, Map<Integer,
                                      MyPair<Condition,byte[]>> fileContent,
                                      ReentrantLock fileContentLock) throws InterruptedException {
        HashMap<InetAddress,ArrayList<Integer>> servers = getServers();
        for (int i = 0; i < numThreads; i++) {
            ChunkThread thread = new ChunkThread(offSetsForThreads.get(i),
                    file,
                    size,
                    packetSize,
                    i == 0 && last,
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
