package additionalTask;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.logging.Logger;

public class Server {

    public static final int PORT = 8080;
    public static LinkedList<ServerThread> serverList = new LinkedList();


    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        Logger logger = Logger.getLogger(Client.class.getName());
        logger.info("Server Started ");
        try {
            while (true) {
                Socket socket = server.accept();
                logger.info("New user connected");
                try {
                    serverList.add(new ServerThread(socket)); // добавить новое соединенние в список
                } catch (IOException e) {
                    socket.close();
                }
            }
        } finally {
            server.close();
        }
    }
}