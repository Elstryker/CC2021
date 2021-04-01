import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class FSChunk {
    // Lista de servidores disponíveis
    HashMap<String,MyPair<InetAddress,Integer>> servers;
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
                    MyPair<InetAddress, Integer> pair = new MyPair<>(packet.getAddress(), packet.getPort());
                    try {
                        lock.lock();
                        // Guarda o novo servidor disponivel
                        servers.put(new String(packet.getData()), pair);
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

    /*
    Info file  :  EXISTS:true SIZE:500 | EXISTS:false
    Get file (off size)
    */

    public byte[] retrieveFile(String cont) {
        DatagramSocket socket = null;
        String metaData;
        try {
            // Criação Socket
            socket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        int i = 0, numServers;
        MyPair<InetAddress,Integer> dest;
        try {
            lock.lock();
            numServers = servers.size();
            do {
                // Enquanto o ficheiro não existir num servidor, ou número de tentativas for ultrapassado continua a tentar encontrar.
                String[] array = (String[]) servers.keySet().toArray();
                dest = servers.get(array[new Random().nextInt(array.length)]);
                metaData = getMetaData(socket, cont, dest);
                i++;
                System.out.println("Try "+ i + " : " + metaData);
            } while (metaData.equals("EXISTS:false") && i < numServers * 2);
        } finally {
            lock.unlock();
        }
        if (i != numServers) {
            FSChunkWorker worker = new FSChunkWorker(socket, "GET " + cont, dest);
            return worker.run();
        }
        else return new byte[1];
    }

    public String getMetaData(DatagramSocket socket,String file, MyPair<InetAddress,Integer> dest) {
        String message = "INFO " + file;
        FSChunkWorker work = new FSChunkWorker(socket,message,dest);
        return new String(work.run());
    }
}
