import javax.crypto.Cipher;
import java.io.IOException;
import java.net.*;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import io.github.cdimascio.dotenv.Dotenv;


public class ServerAuthenticator {
    DatagramSocket socket;
    String hostname = "localhost";
    int port = 12345;
    InetAddress address;


    public ServerAuthenticator(DatagramSocket socket) throws UnknownHostException {
        this.socket = socket;
        this.address = InetAddress.getByName(hostname);
    }

    /*
        Full auth procedure
        1 - FastFileSrv sends byte to HttpGw udp port responsible for auth
        2 - HttpGw answers with it's public key
        3 - FastFileSrv encrypts it's secret using the public key and sends it to the HttpGw
        4 - HttpGw decrypts the secret and checks it against it's own secret. If it matches then it adds the FastFileSrv
            to the list of allowed servers
        5 - HttpGw sends the auth response to the FastFileSrv. If the association was succesful, send the port number the srv socket is connected to, else sends 0
    */
    // Returns the port number the socket is associated to on the HttpGw side
    public int authenticateServer() throws Exception {
        /*
          Sends a byte to the HttpGw, it answers with it's public key encoded in bytes
         */
        byte[] encodedKey = exchangeData(new byte[1]);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(encodedKey));

        /*
         Sends the secret encrypted using the HttpGw's public key
         The server answers with what port the socket is connected to (0 is auth failed)
         */
        Dotenv dotenv = Dotenv.configure()
                .directory("src/main/security")
                .load();
        String authSecret = dotenv.get("AUTH_SECRET");

        byte[] encodedSecret = encrypt(authSecret, publicKey);
        byte[] associatedPort = exchangeData(encodedSecret);

        return Integer.parseInt(new  String(associatedPort, 0, associatedPort.length));
    }

    /*
        Method responsible for sending an UDP packet with the provided data and waiting the the HttpGw's answer
     */
    public byte[] exchangeData(byte[] data) throws IOException {
        while (true) {
            try {
                DatagramPacket request = new DatagramPacket(data, data.length, address, port);
                socket.send(request);

                // waits for the server's answer a maximum of 2 second
                socket.setSoTimeout(2000);
                byte[] buffer = new byte[5000];
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                socket.receive(response);
                socket.setSoTimeout(0);

                byte[] responseBytes = new byte[response.getLength()];
                System.arraycopy(response.getData(), 0, responseBytes, 0, response.getLength());

                return responseBytes;
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
            }
        }
    }


    public static byte[] encrypt(String plainText, PublicKey publicKey) throws Exception {
        //Get Cipher Instance RSA With ECB Mode and OAEPWITHSHA-512ANDMGF1PADDING Padding
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-512ANDMGF1PADDING");

        //Initialize Cipher for ENCRYPT_MODE
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        //Perform Encryption
        return cipher.doFinal(plainText.getBytes());
    }
}
