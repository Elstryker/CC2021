import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class FSChunk {
    // Lista de servidores disponíveis
    HashMap<InetAddress,Integer> servers;
    // Controlo de Concorrência
    ReentrantLock lock;
    // Aceitador de novos servidores
    Thread accepter;

    public FSChunk() {
        servers = new HashMap<>();
        accepter = new Thread(() -> {
            while(true) {
                try {
                    DatagramSocket sock = new DatagramSocket(12300);
                    byte[] content = new byte[100];
                    DatagramPacket packet = new DatagramPacket(content, content.length);
                    sock.setSoTimeout(0);
                    // Aceita novo pedido
                    sock.receive(packet);
                    try {
                        lock.lock();
                        // Guarda o novo servidor disponivel
                        servers.put(packet.getAddress(),packet.getPort());
                    } finally {
                        lock.unlock();
                    }
                    // Confirmação por linha de comando
                    System.out.printf("Accepted: %s, from address: %s, from Port: %s%n\n",new String(packet.getData()),packet.getAddress(),packet.getPort());
                    packet = new DatagramPacket(content, content.length, InetAddress.getByName("localhost"), packet.getPort());
                    sock.send(packet);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        });
    }

    public void start() {
        accepter.start();
    }

    /*
    Info file  :  EXISTS:true SIZE:500 | EXISTS:false
    Get file (off size)
    */

    public byte[] retrieveFile(String cont) throws SocketException, FileNotFoundException {
        DatagramSocket socket;
        String metaData;
        // Criação Socket
        socket = new DatagramSocket();
        int i = 0, numServers;
        MyPair<InetAddress,Integer> dest;
        try {
            lock.lock();
            // Enquanto o ficheiro não existir num servidor, ou número de tentativas for ultrapassado continua a tentar encontrar.
            List<InetAddress> list = new ArrayList<>(servers.keySet());
            int random = new Random().nextInt(list.size());
            dest = new MyPair<>(list.get(random),servers.get(list.get(random)));
            metaData = getMetaData(socket, cont, dest);
        } finally {
            lock.unlock();
        }
        if (metaData.equals("EXISTS:false"))
            throw new FileNotFoundException("File Not Found");
        else {
            FSChunkWorker worker = new FSChunkWorker(socket, "GET " + cont, dest);
            return worker.run();
        }
    }

    public String getMetaData(DatagramSocket socket,String file, MyPair<InetAddress,Integer> dest) {
        String message = "INFO " + file;
        FSChunkWorker work = new FSChunkWorker(socket,message,dest);
        return new String(work.run());
    }
}
