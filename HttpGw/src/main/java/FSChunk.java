import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FSChunk {
    // List of available servers
    private HashMap<InetAddress,Integer> servers;
    // Lock for thread managing
    private ReentrantLock lock;
    // Thread that manages connections from servers
    private Thread accepter;
    // Socket for accepter thread
    private DatagramSocket accepterSocket;

    public FSChunk() throws SocketException {
        servers = new HashMap<>();
        accepterSocket = new DatagramSocket(12345);
        accepter = new Thread(this::accepterWorkFlow);
        lock = new ReentrantLock();
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
                try {
                    lock.lock();
                    // Guarda o novo servidor disponivel
                    servers.put(packet.getAddress(),packet.getPort());
                } finally {
                    lock.unlock();
                }
                // Confirmação por linha de comando
                System.out.printf("Accepted server from Address: %s, from Port: %s%n\n",packet.getAddress(),packet.getPort());
                packet = new DatagramPacket(content, content.length, InetAddress.getByName("localhost"), packet.getPort());
                accepterSocket.send(packet);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /*
    Info file  :  EXISTS:true,SIZE:500,TYPE:type | EXISTS:false
    Get offset size file
    */

    public MyPair<byte[],String> retrieveFile(String fileName) throws SocketException, FileNotFoundException {
        DatagramSocket socket;
        String metaData;
        socket = new DatagramSocket();
        // Get Random Server to fetch files
        MyPair<InetAddress,Integer> server = getServer();
        // Get MetaData Information
        FileMetaData fileMetaData = getMetaData(socket, fileName, server.getFirst(), server.getSecond());
        // Debugging metaData information
        System.out.println("\n" + fileMetaData.toString() + "\n\n");
        // Get file from server if file exists, else throw exception
        if(fileMetaData.fileExists()) {
            byte[] fileContent = getFile(socket,fileName,server.getFirst(),server.getSecond(),fileMetaData.getSize());
            return new MyPair<>(fileContent,fileMetaData.getType());
        }
        else throw new FileNotFoundException("File Not Found");
    }

    private MyPair<InetAddress,Integer> getServer() {
        InetAddress serverAddress;
        int port;
        try {
            lock.lock();
            // Getting keys of the Map as a list
            List<InetAddress> list = new ArrayList<>(servers.keySet());
            // Get a random element from the list
            int random = new Random().nextInt(list.size());
            serverAddress = list.get(random);
            port = servers.get(serverAddress);
        } finally {
            lock.unlock();
        }
        return new MyPair<>(serverAddress,port);
    }

    private byte[] getFile(DatagramSocket socket,String file, InetAddress destAddress,Integer destPort, int size) {
        int packets, offset, i, packetLength = 1024, fileContentSize = 0;
        packets = size / packetLength;
        FSChunkWorker worker = new FSChunkWorker(socket, file, destAddress, destPort);
        byte[] fileContent = new byte[size];
        byte[] fileChunk;
        try {
            for(i = 0, offset = 0; i < packets; i++, offset += packetLength) {
                fileChunk = worker.getFile(offset, packetLength);
                System.arraycopy(fileChunk,0,fileContent,fileContentSize,packetLength);
                fileContentSize += packetLength;
            }
            if((size % packetLength) != 0) {
                fileChunk = worker.getFile(offset,size - offset);
                System.arraycopy(fileChunk,0,fileContent,fileContentSize,size - offset);
            }
        } catch (NoSuchFieldException e) {
            System.out.println(e.getMessage());
        }
        return fileContent;
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
