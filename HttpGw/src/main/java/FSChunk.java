import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FSChunk {
    // Lista de servidores disponíveis
    HashMap<InetAddress,Integer> servers;
    // Controlo de Concorrência
    ReentrantLock lock;
    // Aceitador de novos servidores
    Thread accepter;

    public FSChunk() throws SocketException {
        servers = new HashMap<>();
        DatagramSocket sock = new DatagramSocket(12345);
        accepter = new Thread(() -> {
            while(true) {
                try {
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
                    System.out.printf("Accepted server from Address: %s, from Port: %s%n\n",packet.getAddress(),packet.getPort());
                    packet = new DatagramPacket(content, content.length, InetAddress.getByName("localhost"), packet.getPort());
                    sock.send(packet);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        });
        lock = new ReentrantLock();
    }

    public void start() {
        accepter.start();
    }

    /*
    Info file  :  EXISTS:true,SIZE:500,TYPE:type | EXISTS:false
    Get off size file
    */

    public MyPair<byte[],String> retrieveFile(String cont) throws SocketException, FileNotFoundException {
        DatagramSocket socket;
        String metaData;
        // Criação Socket
        socket = new DatagramSocket();
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
        System.out.println("\n" + metaData + "\n\n");
        Pattern pattern = Pattern.compile("EXISTS:true");
        Matcher matcher = pattern.matcher(metaData);
        if(matcher.find()) {
            String[] temp = metaData.split(",");
            long size = Long.parseLong(temp[1].split(":")[1]);
            String type = temp[2].split(":")[1].trim();
            FSChunkWorker worker = new FSChunkWorker(socket, "GET 0 " + size + " " + cont, dest);
            byte[] ret = worker.run();
            return new MyPair<>(ret,type);
        }
        else throw new FileNotFoundException("File Not Found");
    }

    public String getMetaData(DatagramSocket socket,String file, MyPair<InetAddress,Integer> dest) {
        String message = "INFO " + file;
        FSChunkWorker work = new FSChunkWorker(socket,message,dest);
        byte[] res = work.run();
        return new String(res,0,res.length);
    }
}
