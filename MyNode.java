import java.io.Serializable;

public class MyNode implements Serializable {

    private String name;
    private String ip;
    private int port;

    public MyNode(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public String getIP() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}