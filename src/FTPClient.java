import java.io.*;
import java.net.ConnectException;
import java.net.Socket;

public class FTPClient {

    private Socket requestSocket;
    private String host;
    private int port;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    public FTPClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) {
//        String host = args[0];
//        int port = Integer.parseInt(args[1]);
//        FTPClient client = new FTPClient(host, port);
        FTPClient client = new FTPClient("localhost", 8000);
        client.run();
    }

    private void sendMessage(String message) throws IOException {
        outputStream.writeObject(message);
        outputStream.flush();
    }

//    private void sendMessage(String... message) throws IOException {
//        StringBuilder stringBuilder = new StringBuilder();
//        for (String msg: message) {
//            stringBuilder.append(msg + " ");
//        }
//        outputStream.writeObject(stringBuilder.toString());
//        outputStream.flush();
//    }

    private void uploadFile(String fileName) throws IOException {
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        DataInputStream dataInputStream = null;
        try {
            File file = new File(".//files//client1//" + fileName);
            byte[] buffer = new byte[(int) file.length()];
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            dataInputStream = new DataInputStream(bufferedInputStream);
            dataInputStream.readFully(buffer, 0, buffer.length);

            outputStream.writeUTF(fileName);
            outputStream.writeLong(buffer.length);
            outputStream.write(buffer, 0, buffer.length);
            outputStream.flush();
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find file");
        } finally {
            if (null != fileInputStream) fileInputStream.close();
            if (null != bufferedInputStream) bufferedInputStream.close();
            if (null != dataInputStream) dataInputStream.close();
        }
    }

//    private void receiveFile(String responseStatus) {
//        file
//    }

    void run() {
        try {
            requestSocket = new Socket(host, port);
            System.out.println("Connected to " + host + " on port " + port);
            outputStream = new ObjectOutputStream(requestSocket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(requestSocket.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.println("Input Command");
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
                            sendMessage(command[0]);
                            uploadFile(command[1]);
                        }

                        break;

//                    case "get":
//                        if (command.length < 2) {
//                            System.out.println("No file name specified");
//                        }
//                        else {
//                            sendMessage(command[0]);
//                            sendMessage(command[1]);
//                            String responseStatus = (String) inputStream.readObject();
//                            receiveFile(responseStatus);
//                        }

                    default:
                        break;
                }

            }
        } catch (ConnectException e) {
            System.out.println("Initiate Server");
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