package additionalTask;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;


public class ClientThread {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private BufferedReader inputUser;
    private String addr;
    private int port;
    private boolean flagForQuit;
    Logger logger;


    public ClientThread(String addr, int port) throws ParseException {
        logger = Logger.getLogger(Client.class.getName());
        this.addr = addr;
        this.port = port;
        flagForQuit = false;
        try {
            this.socket = new Socket(addr, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            inputUser = new BufferedReader(new InputStreamReader(System.in));
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            logIn();
            if(!flagForQuit) {
                new ReadMsg().start();
                new WriteMsg().start();
            }
        } catch (IOException e) {
            ClientThread.this.downService();
        }
    }

    public void logIn() throws IOException{
        logger.info("To connect to the chat you need to log in");
        int times = 3;
        String str;
        while(true) {
            if(times == 0){
                waitChoice();
                break;
            }
            enterData();

            try {
                str = in.readUTF(); // ждем сообщения с сервера
                if (str.equals("@true")) {
                    logger.info("Successful authorization. Welcome to the chat");
                    return;
                }
                logger.info(str);
                if(str.equals("User with this name is already on the server. Try to login under a different name.")){
                    continue;
                }


            } catch (IOException e) {
                ClientThread.this.downService();
            }
            times--;
        }
    }

    private void enterData() throws IOException{
        logger.info("Enter login:");
        String login = inputUser.readLine();
        logger.info("Enter password:");
        String pass = inputUser.readLine();
        JSONObject object = new JSONObject();
        object.put("login", login);
        object.put("pass", pass);
        try {
            out.writeUTF(object.toJSONString());
            out.flush();
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
    }

    private void waitChoice() throws IOException{
        logger.info("Enter @new or @quit");
        String str = inputUser.readLine();
        if(str.equals("@new")){
            newuser();
            return;
        }else if(str.equals("@quit")){
            out.writeUTF(str);
            flagForQuit = true;
            ClientThread.this.downService();
            return;
        }
    }

    public void newuser() throws IOException {
        logger.info("New user registration:");
        String message;
        try {
            out.writeUTF("@new");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(true) {
            enterData();
            message = in.readUTF();
            if(message.equals("@success"))
            {
                return;
            }
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
            Logger logger = Logger.getLogger(Client.class.getName());
            String str;
            try {
                while (true) {
                    str = in.readUTF();
                    if (str.equals("@quit")) {
                        ClientThread.this.downService();
                        break;
                    }
                    logger.info(str);
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
                    userWord = inputUser.readLine();
                    if (userWord.equals("@quit")) {
                        out.writeUTF(userWord);
                        ClientThread.this.downService();
                        break;
                    } else {
                        out.writeUTF(userWord);
                    }
                    out.flush();
                } catch (IOException e) {
                    ClientThread.this.downService();

                }

            }
        }
    }
}