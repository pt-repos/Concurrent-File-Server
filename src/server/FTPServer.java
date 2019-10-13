import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FTPServer {

    private static final int sPort = 8000;
    private static final String SUCCESS_RESPONSE_CODE = "SUCCESS";
    private static final String FAILURE_RESPONSE_CODE = "FAILURE";

    private static Set<String> fileSet;
    private static Map<String, String> loginMap;

    public FTPServer() {}

    public static void main(String[] args) throws Exception {
        System.out.println("Server is running");
        ServerSocket listener = new ServerSocket(sPort);
        int clientId = 0;
        fileSet = new HashSet<>();
        loginMap = new HashMap<>();
        loginMap.put("user1", "pass1");
        loginMap.put("user2", "pass2");
        loginMap.put("user3", "pass3");

        try {
            while (true) {
                new Thread(new ConnectionHandler(listener.accept(), ++clientId)).start();
            }
        }
        finally {
            listener.close();
        }
    }

    /**
     * Runnable class for handling individual connections to each client.
     */
    private static class ConnectionHandler implements Runnable {
        private Socket connection;
        private int clientId;
        private ObjectInputStream inputStream;
        private ObjectOutputStream outputStream;

        private ConnectionHandler(Socket connection, int clientId) {
            this.connection = connection;
            this.clientId = clientId;
        }

        /**
         * Send the list of files available in the server.
         * @throws IOException
         */
        private void sendAvailableFiles() throws IOException {
            StringBuffer files = new StringBuffer();
            for (String fileName: fileSet) {
                files.append(fileName + ", ");
            }
            outputStream.writeObject(files.toString());
            outputStream.flush();
        }

        /**
         * receive a file from a client and store it in the server.
         * @param fileName
         * @throws IOException
         */
        private void receiveFile(String fileName) throws IOException {
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
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

        /**
         * transmit a file to the requesting client.
         * @param fileName
         * @throws IOException
         */
        private void sendFile(String fileName) throws IOException {
            FileInputStream fileInputStream = null;
            BufferedInputStream bufferedInputStream = null;
            DataInputStream dataInputStream = null;
            try {
                File file = new File(fileName);
                byte[] buffer = new byte[(int) file.length()];
                fileInputStream = new FileInputStream(file);
                bufferedInputStream = new BufferedInputStream(fileInputStream);
                dataInputStream = new DataInputStream(bufferedInputStream);
                dataInputStream.readFully(buffer, 0, buffer.length);

                outputStream.writeObject(SUCCESS_RESPONSE_CODE);
                outputStream.writeUTF(fileName);
                outputStream.writeLong(buffer.length);
                outputStream.write(buffer, 0, buffer.length);
                outputStream.flush();
            } catch (FileNotFoundException e) {
                System.err.println("Cannot find file " + fileName);
                outputStream.writeObject(FAILURE_RESPONSE_CODE);
                outputStream.flush();
            } finally {
                if (null != dataInputStream) dataInputStream.close();
                if (null != bufferedInputStream) bufferedInputStream.close();
                if (null != fileInputStream) fileInputStream.close();
            }
        }

        /**
         * Verifies credentials received from the client against loginMap.
         * Sends the authentication status to the client.
         * @return boolean result of authentication.
         * @throws IOException
         * @throws ClassNotFoundException
         */
        private boolean authenticateClient() throws IOException, ClassNotFoundException {
            String username = (String) inputStream.readObject();
            String password = (String) inputStream.readObject();

            if (password.equals(loginMap.get(username))) {
                outputStream.writeBoolean(true);
                outputStream.flush();
                return true;
            }
            outputStream.writeBoolean(false);
            outputStream.flush();
            return false;
        }

        public void run() {
            try {
                outputStream = new ObjectOutputStream(connection.getOutputStream());
                outputStream.flush();
                inputStream = new ObjectInputStream(connection.getInputStream());

                // Authenticate username and password
                boolean flag = false;
                while (!flag) {
                    flag = authenticateClient();
                    if (!flag) {
                        System.out.println("Connection refused.");
                    } else {
                        System.out.println("Client " + clientId + " connected");
                    }
                }

                // Listen for commands once client is authenticated
                while (flag) {
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

                            case "get":
                                fileName = (String) inputStream.readObject();
                                sendFile(fileName);
                                break;
                        
                            default:
                                break;
                        }
					} catch (ClassNotFoundException e) {
                        System.err.println("Data received in unknown format");
					}
                }
			} catch (IOException e) {
                System.out.println("Disconnected with Client " + clientId);
            } catch (ClassNotFoundException e) {
                System.err.println("Data received in unknown format");
            }
        }
    }

}