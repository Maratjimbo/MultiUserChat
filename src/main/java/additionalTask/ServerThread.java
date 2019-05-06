package additionalTask;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;

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
        String clientMessage = "";
        String sendName = "";
        boolean flagName = false;
        boolean flagForQuit = false;
        try {
            /*try {
                out.writeUTF("Server:\nHello," + name + "\nAvailable commnd:\n'@senduser name msg' for private message to user\n'@quit' for quit from chat");
                out.flush();
            } catch (IOException ignored) {}*/
            newClient();
            showUser();
            if (clientMessage.equals("@quit")) {
                flagForQuit = true;
            }
            try {
                while (true) {
                    flagName = false;
                    if (!flagForQuit) {
                        clientMessage = in.readUTF();
                    }

                    if (clientMessage.equals("@quit")) {
                        this.downService(); // харакири
                        break;
                    }

                    if (clientMessage.regionMatches(0, "@senduser", 0, 9)) {
                        String tmpmsg = String.copyValueOf(clientMessage.toCharArray(), 10, clientMessage.length() - 10);
                        int i = 0;
                        while (tmpmsg.charAt(i) != ' ') {
                            sendName += tmpmsg.charAt(i);
                            i++;
                        }
                        i++;
                        clientMessage = String.copyValueOf(tmpmsg.toCharArray(), i, tmpmsg.length() - i);
                        flagName = true;
                    }

                    for (ServerThread vr : Server.serverList) {
                        if (flagName) {
                            if (vr.name.equals(sendName)) {
                                vr.send(clientMessage, name, true);
                                break;
                            }
                        } else {
                            vr.send(clientMessage, name, false);
                        }
                    }
                }
            } catch (NullPointerException ignored) {
            }


        } catch (IOException e) {
            this.downService();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void send(String msg, String name, boolean flagForPrivate) {
        try {
            if (flagForPrivate) {
                out.writeUTF("Private message from " + name + " : " + msg);
            } else {
                out.writeUTF(name + " : " + msg);
            }
            out.flush();
        } catch (IOException ignored) {
        }

    }

    private void newClient() throws ParseException {
        int times = 3;
        int flagQuit = 0;
        String clientmaessage = "";
        while (true) {
            try {
                clientmaessage = in.readUTF();
            } catch (IOException e) {
                e.printStackTrace();
            }
            JSONParser parser = new JSONParser();
            JSONObject object = (JSONObject) parser.parse(clientmaessage);
            try {
                JSONArray database = (JSONArray) parser.parse(new FileReader("database.json"));
                flagQuit = 0;
                flagQuit = verification(database, object);
                if(flagQuit == 1){
                    return;
                }
                if (flagQuit == 2) {
                    continue;
                }
                times--;
                if (times != 0) {
                    out.writeUTF("Login or password is incorrect. Try filling them in again.\nYou can try " + times
                            + " more times.");
                } else {
                    out.writeUTF("You have exceeded the number of login attempts. It is possible that you are not " +
                            "registered in the chat. To register, enter @new, or @quit for out form the chat");
                }
                out.flush();
                if (times == 0) {
                    clientmaessage = in.readUTF();
                    if (clientmaessage.equals("@new")) {
                        addNewUser();
                    } else if (clientmaessage.equals("@quit")) {
                        this.downService();
                    }
                    return;
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int verification(JSONArray database, JSONObject object) throws IOException{
        boolean onchat = false;
        for (Object obj : database) {
            if (((JSONObject) obj).get("login").equals(object.get("login"))) {
                if (((JSONObject) obj).get("pass").equals(object.get("pass"))) {
                    for (ServerThread vr : Server.serverList) {
                        if (vr.getNick() != null) {
                            if (vr.getNick().equals(object.get("login"))) {
                                onchat = true;
                            }
                        }
                    }
                    if (!onchat) {
                        name = (String) object.get("login");
                        out.writeUTF("@true");
                        out.flush();
                        return 1;
                    } else {
                        out.writeUTF("User with this name is already on the server. Try to login under a different name.");
                        out.flush();
                        return 2;
                    }
                }
            }
        }
        return 0;
    }


    private void addNewUser() throws ParseException {
        String clientmaessage = "";
        boolean flagForUse = false;
        while (true) {
            flagForUse = false;
            try {
                clientmaessage = in.readUTF();
            } catch (IOException e) {
                e.printStackTrace();
            }
            JSONParser parser = new JSONParser();
            JSONObject object = (JSONObject) parser.parse(clientmaessage);
            try {
                JSONArray database = (JSONArray) parser.parse(new FileReader("database.json"));
                for (Object obj : database) {
                    if (((JSONObject) obj).get("login").equals(object.get("login"))) {
                        out.writeUTF("This login is busy. Try another");
                        flagForUse = true;
                        out.flush();
                    }
                }
                if (!flagForUse) {
                    out.writeUTF("@success");
                    name = (String) object.get("login");
                    out.flush();
                    refreshBase(object);
                    return;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void refreshBase(JSONObject object) {
        JSONParser parser = new JSONParser();
        try {
            JSONArray database = (JSONArray) parser.parse(new FileReader("database.json"));
            database.add(object);
            FileWriter file = new FileWriter("database.json");
            file.write(database.toJSONString());
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    private void showUser() throws IOException {
        String allUser = "";
        int i = 0;
        for (ServerThread vr : Server.serverList) {
            if (vr.name != null) {
                if (i != 0) {
                    allUser += ", ";
                }
                allUser += vr.name;
                i++;
            }
        }
        out.writeUTF("Users on chat : " + allUser);
        out.flush();
    }

    private void downService() {
        boolean unknownUser = false;
        try {
            if (!socket.isClosed()) {
                if (name == null) {
                    unknownUser = true;
                }

                if (!unknownUser) {
                    String message = name;
                    message += " was disconnected";
                    for (ServerThread vr : Server.serverList) {
                        if (!vr.equals(this)) {
                            vr.send(message, "Server", false);
                        }
                    }
                    Server.serverList.remove(this);
                }
                socket.close();
                in.close();
                out.close();
                interrupt();
            }
        } catch (IOException ignored) {
        }
    }

    public String getNick() {
        return name;
    }
}