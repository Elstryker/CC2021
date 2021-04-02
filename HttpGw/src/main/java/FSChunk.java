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
        metaData = getMetaData(socket, fileName, server.getFirst(), server.getSecond());
        // Debugging metaData information
        System.out.println("\n" + metaData + "\n\n");
        FileMetaData fileMetaData = new FileMetaData(metaData);
        // Get file from server if file exists, else throw exception
        if(fileMetaData.fileExists()) {
            byte[] fileContent = getFile(socket,fileName,server.getFirst(),server.getSecond(),fileMetaData.getSize());
            return new MyPair<>(fileContent,fileMetaData.getType());
        }
        else throw new FileNotFoundException("File Not Found");
    }

    public MyPair<InetAddress,Integer> getServer() {
        InetAddress serverAddress;
        int port;
        try {
            lock.lock();
            List<InetAddress> list = new ArrayList<>(servers.keySet());
            int random = new Random().nextInt(list.size());
            serverAddress = list.get(random);
            port = servers.get(serverAddress);
        } finally {
            lock.unlock();
        }
        return new MyPair<>(serverAddress,port);
    }

    public byte[] getFile(DatagramSocket socket,String file, InetAddress destAddress,Integer destPort, long size) {
        FSChunkWorker worker = new FSChunkWorker(socket, destAddress, destPort);
        return worker.getFile(file,0,(int) size);
    }

    public String getMetaData(DatagramSocket socket,String file, InetAddress destAddress,Integer destPort) {
        FSChunkWorker work = new FSChunkWorker(socket,destAddress,destPort);
        byte[] result = work.getMetaData(file);
        return new String(result);
    }
}
