package additionalTask;


import org.json.simple.parser.ParseException;

public class Client {

    public static String ipAddr = "localhost";
    public static int port = 8080;


    public static void main(String[] args) throws ParseException {
        new ClientThread(ipAddr, port);
    }
}