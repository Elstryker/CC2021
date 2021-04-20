package FSChunk;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ServerExitWorker implements Runnable {
    // Socket for the thread thread
    private final DatagramSocket accepterSocket;
    // List of available servers
    private final HashMap<InetAddress, ArrayList<Integer>> servers;
    // Lock for thread managing on servers structure
    private final ReentrantLock serversLock;

    public ServerExitWorker(HashMap<InetAddress, ArrayList<Integer>> servers,
                            ReentrantLock serversLock) throws SocketException {
        this.accepterSocket = new DatagramSocket(54321);
        this.servers = servers;
        this.serversLock = serversLock;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Receive exit request
                DatagramPacket exitRequest = new DatagramPacket(new byte[1], 1);
                accepterSocket.receive(exitRequest);
                InetAddress srvAddress = exitRequest.getAddress();
                int srvPort = exitRequest.getPort();

                // Remove server from list of available servers
                deleteServer(srvAddress, srvPort);

                DatagramPacket confirmation = new DatagramPacket(new byte[1], new byte[1].length,
                        srvAddress, srvPort);
                accepterSocket.send(confirmation);
                System.out.printf("Deleted server from Address: %s, from Port: %s%n\n", exitRequest.getAddress(), exitRequest.getPort());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteServer(InetAddress address, int port) {
        try {
            serversLock.lock();
            // Remove server form the structure
            if (servers.containsKey(address)) {
                ArrayList<Integer> ports = servers.get(address);
                if (ports.size() > 1) {
                    ports.remove((Integer) port);
                } else {
                    servers.remove(address);
                }
            }
        } finally {
            serversLock.unlock();
        }
    }
}
