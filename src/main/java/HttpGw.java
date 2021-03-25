import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpGw {
    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(12345);
        while (true) {
            Socket socket = server.accept();
            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String request = br.readLine();
            String[] requestParam = request.split(" ");
            Pattern pattern = Pattern.compile(("/(.+)"));
            Matcher matcher = pattern.matcher(requestParam[1]);
            String path;
            if(matcher.find())
                path = matcher.group(1);
            else path = requestParam[1];

            /*
            File file = new File(path);
            if(!file.exists()) {
                PrintWriter outP = new PrintWriter(socket.getOutputStream(),true);
                outP.write("HTTP/1.1 404 Not Found");
                outP.flush();
                outP.close();
            }
            else {
                OutputStream out = socket.getOutputStream();
                InputStream fileIS = new FileInputStream(file);
                long length = file.length();
                byte[] bytes = new byte[16 * 1024];
                int count;
                while ((count = fileIS.read(bytes)) > 0)
                    out.write(bytes, 0, count);
                out.flush();
                out.close();
                fileIS.close();
            }
            */
            br.close();
            socket.close();
        }
    }
}
