

import java.io.*;
import java.net.Socket;

public class ClientThread {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private BufferedReader inputUser;
    private String addr;
    private int port;
    private String nickname;


    public ClientThread(String addr, int port) {
        this.addr = addr;
        this.port = port;
        try {
            this.socket = new Socket(addr, port);
        } catch (IOException e) {
            System.err.println("Socket failed");
        }
        try {
            inputUser = new BufferedReader(new InputStreamReader(System.in));
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            setName();
            new ReadMsg().start();
            new WriteMsg().start();
        } catch (IOException e) {
            ClientThread.this.downService();
        }
    }

    public void setName() {
        System.out.println("Enter your nickname on server:");
        try {
            nickname = inputUser.readLine();
            out.writeUTF(nickname);
            out.flush();
        } catch (IOException ignored) {
        }

    }

    private void downService() {
        try {
            if (!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
            }
        } catch (IOException ignored) {}
    }

    private class ReadMsg extends Thread {
        @Override
        public void run() {
            String str;
            try {
                while (true) {
                    str = in.readUTF(); // ждем сообщения с сервера
                    if (str.equals("stop")) {
                        ClientThread.this.downService(); // харакири
                        break; // выходим из цикла если пришло "stop"
                    }
                    System.out.println(str); // пишем сообщение с сервера на консоль
                }
            } catch (IOException e) {
                ClientThread.this.downService();
            }
        }
    }


    public class WriteMsg extends Thread {
        @Override
        public void run() {
            while (true) {
                String userWord;
                try {
                    userWord = inputUser.readLine(); // сообщения с консоли
                    if (userWord.equals("@quit")) {
                        out.writeUTF(userWord);
                        ClientThread.this.downService(); // харакири
                        break; // выходим из цикла если пришло "stop"
                    } else {
                        out.writeUTF(userWord); // отправляем на сервер
                    }
                    out.flush(); // чистим
                } catch (IOException e) {
                    ClientThread.this.downService(); // в случае исключения тоже харакири

                }

            }
        }
    }
}