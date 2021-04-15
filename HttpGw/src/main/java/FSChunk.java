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
    // Socket Pool Lock
    private ReentrantLock socketsLock;
    // Condition for threads waiting to use sockets
    private Condition socketsCondition;
    // Thread that manages connections from servers
    private Thread accepter;
    // Socket for accepter thread
    private DatagramSocket accepterSocket;
    // Socket Pool that maps a socket to a state that determines if it's being used or not (True -> Available, False -> Unavailable)
    private HashMap<DatagramSocket,Boolean> sockets;
    // Max number of sockets
    private int maxSockets;


    public FSChunk() throws SocketException {
        servers = new HashMap<>();
        accepterSocket = new DatagramSocket(12345);
        accepter = new Thread(this::accepterWorkFlow);
        serversLock = new ReentrantLock();
        socketsLock = new ReentrantLock();
        maxSockets = 1000;
        sockets = new HashMap<>();
        for(int i = 0; i < maxSockets; i++) {
            sockets.put(new DatagramSocket(),true);
        }
        socketsCondition = socketsLock.newCondition();
    }

    public void start() {
        accepter.start();
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
            // Guarda o novo servidor disponivel
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
            // Retira o servidor da estrutura
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
        DatagramSocket socket = getSocket();
        // Get Random Server to fetch files
        MyPair<InetAddress,Integer> server = getServer();
        // Get MetaData Information
        FileMetaData fileMetaData = getMetaData(socket, fileName, server.getFirst(), server.getSecond());
        releaseSocket(socket);
        // Debugging metaData information
        System.out.println("\n" + fileMetaData.toString() + "\n\n");
        // Get file from server if file exists, else throw exception
        if(fileMetaData.fileExists()) {
            byte[] fileContent = getFile(fileName,fileMetaData.getSize());
            return new MyPair<>(fileContent,fileMetaData.getType());
        }
        else throw new FileNotFoundException("File Not Found");
    }

    private MyPair<InetAddress,Integer> getServer() {
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
        byte[] fileContent = new byte[size];
        try {
            int offset = 0, packetLength = 1024, fileContentSize = 0;
            int packets = size / packetLength;
            DatagramSocket socket;
            // Get socket from pool
            socket = getSocket();
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
                releaseSocket(socket);
            }
        } catch (NoSuchFieldException e) {
            System.out.println(e.getMessage());
        }
        return fileContent;
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

    private DatagramSocket getSocket() {
        // Get a socket from the pool and update its state
        DatagramSocket socket = null;
        try {
            socketsLock.lock();
            while(socket == null) {
                for (Map.Entry<DatagramSocket, Boolean> entry : sockets.entrySet()) {
                    if (entry.getValue()) {
                        socket = entry.getKey();
                        sockets.put(socket, false);
                        break;
                    }
                }
                // If there wasn't any socket available, thread waits
                if(socket == null)
                    socketsCondition.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            socketsLock.unlock();
        }
        return socket;
    }

    private void releaseSocket(DatagramSocket socket) {
        try {
            // Update socket availability to true
            socketsLock.lock();
            sockets.put(socket,true);
            // Wakes up any threads waiting for sockets
            socketsCondition.signalAll();
        } finally {
            socketsLock.unlock();
        }

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
