import javax.crypto.Cipher;
import java.io.IOException;
import java.net.*;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
// TODO: QUITTER
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
        5 - HttpGw sends the auth response to the FastFileSrv: "Granted"/"Denied"
    */
    public boolean authenticateServer() throws Exception {
        /*
          Sends a byte to the HttpGw, it answers with it's public key encoded in bytes
         */
        byte[] encodedKey = exchangeData(new byte[1]);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(encodedKey));

        /*
         Sends the secret encrypted using the HttpGw's public key
         */
        String secret = "Plain text which need to be encrypted by Java RSA Encryption in ECB Model";
        byte[] encodedSecret = encrypt(secret, publicKey);
        byte[] authResponseBytes = exchangeData(encodedSecret);
        String authResponse = new String(authResponseBytes, 0, authResponseBytes.length);

        return authResponse.equals("Granted");
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
