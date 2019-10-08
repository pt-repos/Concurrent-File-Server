import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

public class FTPClient {

    Socket requestSocket;
    String host;
    int port;

    public FTPClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        FTPClient client = new FTPClient(host, port);
        client.run();
    }

    void run() {
        try {
            requestSocket = new Socket(host, port);
            System.out.println("Connected to " + host + "on port " + port);
        } catch(ConnectException e) {
            System.out.println("Initiate Server");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != requestSocket) {
                    requestSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}