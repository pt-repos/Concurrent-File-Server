import java.net.ServerSocket;
import java.net.Socket;

public class FTPServer {
    
    private static final int sPort = 8000;

    public static void main(String[] args) throws Exception {
        System.out.println("Server is running");
        ServerSocket listener = new ServerSocket(sPort);
        int clientId = 0;
        try {
            while(true) {
                new Handler(listener.accept(), ++clientId).start();
                System.out.println("Client " + clientId + "connected");
            }
        }
        finally {
            listener.close();
        }
    }

    private static class Handler extends Thread {
        private Socket connection;
        private int clientId;

        private Handler(Socket connection, int clientId) {
            this.connection = connection;
            this.clientId = clientId;
        }

        public void run() {

        }
    }

}