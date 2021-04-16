package Utils;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SocketPool {
    // Socket Pool Lock
    private ReentrantLock socketsLock;
    // Condition for threads waiting to use sockets
    private Condition socketsCondition;
    // Socket Pool that maps a socket to a state that determines if it's being used or not (True -> Available, False -> Unavailable)
    private HashMap<DatagramSocket, Boolean> sockets;
    // Max number of sockets
    private int maxSockets;

    public SocketPool(int socketMax) throws SocketException {
        socketsLock = new ReentrantLock();
        maxSockets = socketMax;
        sockets = new HashMap<>();
        for (int i = 8000; i < maxSockets + 8000; i++) {
            sockets.put(new DatagramSocket(i), true);
        }
        socketsCondition = socketsLock.newCondition();
    }

    public DatagramSocket getSocket() {
        // Get a socket from the pool and update its state
        DatagramSocket socket = null;
        try {
            socketsLock.lock();
            while (socket == null) {
                for (Map.Entry<DatagramSocket, Boolean> entry : sockets.entrySet()) {
                    if (entry.getValue()) {
                        socket = entry.getKey();
                        sockets.put(socket, false);
                        break;
                    }
                }
                // If there wasn't any socket available, thread waits
                if (socket == null)
                    socketsCondition.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            socketsLock.unlock();
        }
        return socket;
    }

    public void releaseSocket(DatagramSocket socket) {
        try {
            // Update socket availability to true
            socketsLock.lock();
            sockets.put(socket, true);
            // Wakes up any threads waiting for sockets
            socketsCondition.signalAll();
        } finally {
            socketsLock.unlock();
        }

    }
}
