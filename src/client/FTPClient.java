import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

public class FTPClient {

    private static final String SUCCESS_RESPONSE_CODE = "SUCCESS";

    private Socket requestSocket;
    private String host;
    private int port;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private static BufferedReader bufferedReader;

    public FTPClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) {
        String host;
        int port;
        bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        boolean connectionSuccessful = false;
        do {
            try {

                System.out.println("Enter command:");
                String[] command = bufferedReader.readLine().split(" ");

                switch (command[0]) {
                    case "ftpclient":
                        if (command.length < 3) {
                            System.out.println("Not enough arguments");
                        }
                        else {
                            host = command[1];
                            port = Integer.parseInt(command[2]);

                            FTPClient client = new FTPClient(host, port);
                            client.run();
                            connectionSuccessful = true;
                        }
                        break;

                    case "dir":
                    case "upload":
                    case "get":
                        System.out.println("Use \"ftpclient <ip> <port>\" to connect to server first");
                        break;

                    default:
                        System.out.println("Unknown command. Please try again.");
                }
            } catch (ConnectException e) {
                System.out.println("Initiate Server");
            } catch (UnknownHostException e) {
                System.out.println("Unknown host");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Try again.");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while (!connectionSuccessful);

        try {
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a String message on the outputStream.
     * @param message the String message to be transmitted to the server.
     * @throws IOException
     */
    private void sendMessage(String message) throws IOException {
        outputStream.writeObject(message);
        outputStream.flush();
    }

    /**
     * Appends variable number of String arguments and sends the message
     * on the outputStream.
     * @param messages the array of String messages to be transmitted to the server.
     * @throws IOException
     */
    private void sendMessage(String... messages) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        for (String msg: messages) {
            stringBuilder.append(msg + " ");
        }
        outputStream.writeObject(stringBuilder.toString());
        outputStream.flush();
    }

    /**
     * Sends the specified fileName on the output stream if available, else displays
     * error message on standard output.
     * @param fileName the name of file to be uploaded to the server.
     * @throws IOException
     */
    private void uploadFile(String fileName) throws IOException {
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

            outputStream.writeObject("upload");
            outputStream.writeUTF(fileName);
            outputStream.writeLong(buffer.length);
            outputStream.write(buffer, 0, buffer.length);
            outputStream.flush();
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find file " + fileName);
        } finally {
            if (null != fileInputStream) fileInputStream.close();
            if (null != bufferedInputStream) bufferedInputStream.close();
            if (null != dataInputStream) dataInputStream.close();
        }
    }

    /**
     * Based on the responseCode, either receives a file from the server and stores it locally or
     * displays a error message on standard output.
     * @param responseCode the responseCode received from the server.
     * @throws IOException
     */
    private void receiveFile(String responseCode) throws IOException {
        if (SUCCESS_RESPONSE_CODE.equals(responseCode)) {
            String fileName = inputStream.readUTF();
            Long fileSize = inputStream.readLong();
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while (fileSize > 0
                    && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {

                fileOutputStream.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }
            fileOutputStream.close();
        } else {
            System.err.println("File not found on server");
        }
    }

    /**
     * Accepts username and password from the Standard Input, sends the values to server,
     * receives authentication status from the server and returns it.
     * @return boolean result of authentication
     * @throws IOException
     */
    private boolean authenticateClient() throws IOException {

        System.out.println("Enter Username: ");
        String username = bufferedReader.readLine();
        System.out.println("Enter password: ");
        String password = bufferedReader.readLine();

        outputStream.writeObject(username);
        outputStream.writeObject(password);
        outputStream.flush();

        boolean res = inputStream.readBoolean();
        return res;
    }

    /**
     *
     */
    void run() throws ConnectException, UnknownHostException {
        try {
            requestSocket = new Socket(host, port);
            outputStream = new ObjectOutputStream(requestSocket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(requestSocket.getInputStream());

            // Authenticate username and password
            boolean flag = false;
            while (!flag) {
                flag = authenticateClient();
                if (!flag) {
                    System.out.println("Connection refused. Invalid username or password.");
                } else {
                    System.out.println("Connected to " + host + " on port " + port);
                }
            }

            // Send commands to server once authenticated
            while (flag) {
                System.out.println("Enter Command:");
                String[] command = bufferedReader.readLine().split(" ");
                switch (command[0]) {
                    case "dir":
                        sendMessage(command[0]);
                        String files = (String) inputStream.readObject();
                        System.out.println("Available files: " + files);
                        break;

                    case "upload":
                        if (command.length < 2) {
                            System.out.println("No File name specified");
                        }
                        else {
                            uploadFile(command[1]);
                        }
                        break;

                    case "get":
                        if (command.length < 2) {
                            System.out.println("No file name specified");
                        }
                        else {
                            sendMessage(command[0]);
                            sendMessage(command[1]);
                            String responseCode = (String) inputStream.readObject();
                            receiveFile(responseCode);
                        }
                        break;
                        
                    case "ftpclient":
                        System.out.println("Already connected to server \"" + host + ":" + port + "\"");
                        break;

                    default:
                        System.out.println("Unknown command. Please try again.");
                        break;
                }
            }
        } catch (ConnectException e) {
            throw new ConnectException();
        } catch (UnknownHostException e) {
            throw new UnknownHostException();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != requestSocket) requestSocket.close();
                if (null != outputStream) outputStream.close();
                if (null != inputStream) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
