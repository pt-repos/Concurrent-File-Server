import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class FTPServer {

    private static final int sPort = 8000;
    private static Set<String> fileSet;
    
    public FTPServer() {}

    public static void main(String[] args) throws Exception {
        System.out.println("Server is running");
        ServerSocket listener = new ServerSocket(sPort);
        int clientId = 0;

        fileSet = new HashSet<>();
        fileSet.add("File1");
        fileSet.add("File2");

        try {
            while (true) {
                new Handler(listener.accept(), ++clientId).start();
                System.out.println("Client " + clientId + " connected");
            }
        }
        finally {
            listener.close();
        }
    }

    private static class Handler extends Thread {
        private Socket connection;
        private int clientId;
        private ObjectInputStream inputStream;
        private ObjectOutputStream outputStream;
        private FileInputStream fileInputStream;
        private FileOutputStream fileOutputStream;

        private Handler(Socket connection, int clientId) {
            this.connection = connection;
            this.clientId = clientId;
        }

        private void sendAvailableFiles() throws IOException {
            StringBuffer files = new StringBuffer();
            for (String fileName: fileSet) {
                files.append(fileName + ", ");
            }
            outputStream.writeObject(files.toString());
            outputStream.flush();
        }

        private void receiveFile(String fileName) throws IOException {
            fileOutputStream = new FileOutputStream(".//files//server//" + fileName);
            Long fileSize = inputStream.readLong();
            byte[] buffer = new byte[1024];

            int bytesRead;
            while (fileSize > 0
                    && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {

                fileOutputStream.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }
            fileOutputStream.close();
            fileSet.add(fileName);
        }

        public void run() {
            try {
                outputStream = new ObjectOutputStream(connection.getOutputStream());
                outputStream.flush();
                inputStream = new ObjectInputStream(connection.getInputStream());

                while (true) {
                    try {
                        String command = (String) inputStream.readObject();
                        System.out.println(command + " command received from client" + clientId);

                        switch (command) {
                            case "dir":
                                sendAvailableFiles();
                                break;

                            case "upload":
                                String fileName = inputStream.readUTF();
                                receiveFile(fileName);
                                break;

//                            case "get":
                        
                            default:
                                System.out.println("Unknown command. Please try again.");
                                break;
                        }
					} catch (ClassNotFoundException e) {
                        System.err.println("Data received in unknown format");
					}
                }
			} catch (IOException e) {
                System.out.println("Disconnected with Client " + clientId);
            }
        }
    }

}