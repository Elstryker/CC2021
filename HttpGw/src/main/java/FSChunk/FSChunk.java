package FSChunk;

import Utils.MyPair;
import Utils.SocketPool;

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
