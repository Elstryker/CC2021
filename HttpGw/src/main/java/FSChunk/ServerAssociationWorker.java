package FSChunk;

import io.github.cdimascio.dotenv.Dotenv;

import javax.crypto.Cipher;
import java.io.IOException;
import java.net.*;
import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ServerAssociationWorker implements Runnable{
    // Socket for accepter thread
    private final DatagramSocket accepterSocket;
    // List of available servers
    private final HashMap<InetAddress,ArrayList<Integer>> servers;
    // Lock for thread managing on servers structure
    private final ReentrantLock serversLock;
    // Public-private key pair for auth secret encryption
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public ServerAssociationWorker(HashMap<InetAddress,ArrayList<Integer>> servers,
                                   ReentrantLock serversLock) throws SocketException, NoSuchAlgorithmException {
        this.accepterSocket = new DatagramSocket(12345);
        this.servers = servers;
        this.serversLock = serversLock;

        /// Key pair creation
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }

    /*
       Full auth procedure
       1 - FastFileSrv sends byte to HttpGw udp port responsible for auth
       2 - HttpGw answers with it's public key
       3 - FastFileSrv encrypts it's secret using the public key and sends it to the HttpGw
       4 - HttpGw decrypts the secret and checks it against it's own secret. If it matches then it adds the FastFileSrv
           to the list of allowed servers
       5 - HttpGw sends the auth response to the FastFileSrv: "Granted"/"Denied"
   */
    @Override
    public void run(){
        Dotenv dotenv = Dotenv.configure()
                .directory("src/main/security")
                .load();
        String authSecret = dotenv.get("AUTH_SECRET");

        while(true) {
            try {
                /// Receive the auth request
                DatagramPacket authRequest = new DatagramPacket(new byte[1], 1);
                System.out.println ("Listening on port: 8080");

                accepterSocket.receive(authRequest); //The receive() method blocks until a datagram is received. And the following code sends a DatagramPacket to the client:
                InetAddress fastFileSrvAddress = authRequest.getAddress();

                int srvPort = authRequest.getPort();

                /// Sends public key to the server trying to connect. The server should answer it the auth secret encrypted using the public key
                byte[] encodedPublicKey = publicKey.getEncoded();
                byte[] encodedSecretBytes = exchangeData(encodedPublicKey, fastFileSrvAddress, srvPort);

                /// Decrypt secret and check it against the HttpGw secret
                String decryptedSecret = decrypt(encodedSecretBytes, privateKey);
                byte[] authFinalResponse;
                if (decryptedSecret.equals(authSecret)) {
                    // Send the httpGW auth veredict
                    authFinalResponse = "Granted".getBytes();
                    saveServer(fastFileSrvAddress, srvPort);
                    System.out.printf("Accepted server from Address: %s, from Port: %s%n\n",fastFileSrvAddress,srvPort);
                } else {
                     authFinalResponse = "Denied".getBytes();
                }

                DatagramPacket authFinalResponsePacket = new DatagramPacket(authFinalResponse, authFinalResponse.length,
                        fastFileSrvAddress, srvPort);
                accepterSocket.send(authFinalResponsePacket);
            } catch (Exception e){
                e.printStackTrace ();
            }
        }
    }

    /*
   Method responsible for sending an UDP packet with the provided data and waiting the the HttpGw's answer
*/
    public byte[] exchangeData(byte[] data, InetAddress address, int port) throws SocketTimeoutException, SocketException {
        int timeoutCounter=0;
        while (timeoutCounter<3) {
            try {
                DatagramPacket request = new DatagramPacket(data, data.length, address, port);
                accepterSocket.send(request);

                byte[] buffer = new byte[5000];
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                // waits for the server's answer a maximum of 2 second
                accepterSocket.setSoTimeout(2000);
                accepterSocket.receive(response);
                accepterSocket.setSoTimeout(0);

                byte[] responseBytes = new byte[response.getLength()];
                System.arraycopy(response.getData(), 0, responseBytes, 0, response.getLength());

                return responseBytes;
            } catch (IOException e) { // Includes SocketTimeoutException
                accepterSocket.setSoTimeout(0);
                timeoutCounter++;
            }
        }
        accepterSocket.setSoTimeout(0);
        throw new SocketTimeoutException();
    }

    public static String decrypt(byte[] cipherTextArray, PrivateKey privateKey) throws Exception
    {
        //Get Cipher Instance RSA With ECB Mode and OAEPWITHSHA-512ANDMGF1PADDING Padding
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-512ANDMGF1PADDING");

        //Initialize Cipher for DECRYPT_MODE
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        //Perform Decryption
        byte[] decryptedTextArray = cipher.doFinal(cipherTextArray);
        return new String(decryptedTextArray);
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
}
