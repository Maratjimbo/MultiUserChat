
import java.io.*;
import java.net.Socket;

class ServerThread extends Thread {

    private DataInputStream in;
    private DataOutputStream out;
    private Socket socket;
    private String name;


    public ServerThread(Socket socket) throws IOException {
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        start();
    }

    @Override
    public void run() {
        String clientMessage;
        String sendName = "";
        boolean flagName = false;
        try {
            clientMessage = in.readUTF();
            name = clientMessage;
            //System.out.println(clientMessage);
            try {
                out.writeUTF("Server:\nHello," + name + "\nAvailable commnd:\n'@senduser name msg' for private message to user\n'@quit' for quit from chat");
                out.flush();
            } catch (IOException ignored) {}
            newClient();
            showUser();
            try {
                while (true) {
                    flagName = false;
                    clientMessage = in.readUTF();

                    if(clientMessage.equals("@quit")) {
                        this.downService(); // харакири
                        break; // если пришла пустая строка - выходим из цикла прослушки
                    }

                    if(clientMessage.regionMatches(0, "@senduser", 0, 9)){
                        String tmpmsg = String.copyValueOf(clientMessage.toCharArray(), 10, clientMessage.length() - 10);
                        int i = 0;
                        while(tmpmsg.charAt(i) != ' '){
                            sendName += tmpmsg.charAt(i);
                            i++;
                        }
                        i++;
                        clientMessage = String.copyValueOf(tmpmsg.toCharArray(), i, tmpmsg.length() - i);
                        flagName = true;
                        //System.out.println(name + " to " + sendName + " : " + clientMessage);
                    }

                    for (ServerThread vr : Server.serverList) {
                        if(flagName){
                            if(vr.name.equals(sendName)){
                                vr.send(clientMessage, name,true);
                                break;
                            }
                        }else {
                            vr.send(clientMessage, name,false);
                        }
                    }
                }
            } catch (NullPointerException ignored) {}


        } catch (IOException e) {
            this.downService();
        }
    }

    private void send(String msg, String name, boolean flagForPrivate) {
        try {
            if(flagForPrivate){
                out.writeUTF("PRIVATE MESSAGE FROM " + name + " : " + msg);
            }else {
                out.writeUTF(name + " : " + msg);
            }
            out.flush();
        } catch (IOException ignored) {}

    }

    private void newClient(){
        String tmp = " was connected to chat";
        String message = name;
        message += tmp;
        for (ServerThread vr : Server.serverList) {
            if(!vr.equals(this)) {
                vr.send(message, "Server", false);
            }
        }
    }

    private void showUser() throws IOException {
        String allUser = "";
        int i = 0;
        for (ServerThread vr : Server.serverList) {
            if(i != 0){
                allUser += ", ";
            }
            allUser += vr.name;
            i++;
        }
        out.writeUTF("Users on chat : " + allUser);
        out.flush();
    }

    private void downService() {
        try {
            if(!socket.isClosed()) {
                String message = name;
                message += " was disconnectd";
                for (ServerThread vr : Server.serverList) {
                    if(!vr.equals(this)) {
                        vr.send(message, "Server", false);
                    }
                }
                socket.close();
                in.close();
                out.close();
                interrupt();
            }
        } catch (IOException ignored) {}
    }

    public String getNick() {
        return name;
    }
}