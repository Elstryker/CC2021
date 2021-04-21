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
                byte[] exitRequest = new byte[10];
                DatagramPacket exitRequestPacket = new DatagramPacket(exitRequest, exitRequest.length);
                accepterSocket.receive(exitRequestPacket);

                byte[] portNumber = new byte[exitRequestPacket.getLength()];
                System.arraycopy(exitRequestPacket.getData(), 0, portNumber, 0, exitRequestPacket.getLength());

                InetAddress srvAddress = exitRequestPacket.getAddress();
                int srvPortFromRequest = exitRequestPacket.getPort();
                // Can't use the port directly on the packet but the one that is sent in the message,
                // since the exit request comes from a different socket
                int srvPortToClose = Integer.parseInt(new  String(portNumber, 0, portNumber.length));

                // Remove server from list of available servers
                deleteServer(srvAddress, srvPortToClose);
                DatagramPacket confirmation = new DatagramPacket(new byte[1], new byte[1].length,
                        srvAddress, srvPortFromRequest);
                accepterSocket.send(confirmation);
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
                    System.out.printf("Deleted server from Address: %s, from Port: %s%n\n", address, address);
                } else if(ports.get(0).equals(port)){
                    servers.remove(address);
                    System.out.printf("Deleted server from Address: %s, from Port: %s%n\n", address, address);
                }
            }
        } finally {
            serversLock.unlock();
        }
    }
}
