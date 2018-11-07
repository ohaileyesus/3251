import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Map;

public class SendRTT implements  Runnable{

    private String nodeName;
    private Map<String, MyNode> knownNodes;
    DatagramSocket socket;


    public SendRTT(String nodeName, DatagramSocket socket, Map<String, MyNode> knownNodes) {
        this.nodeName = nodeName;
        this.socket = socket;
        this.knownNodes = knownNodes;
    }

    public void run() {


        try {

            while (true) {

                for (String name : knownNodes.keySet()) {

                    MyNode myNode = knownNodes.get(name);

                    InetAddress ipAddress = InetAddress.getByAddress(myNode.getIP().getBytes());

                    byte[] message = preparePacket(myNode);

                    DatagramPacket sendPacket = new DatagramPacket(message, message.length, ipAddress, myNode.getPort());

                    socket.send(sendPacket);

                }
                try {
                    Thread.sleep(5000);
                }  catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }





    }

    public byte[] preparePacket(MyNode myNode) {

        byte[] packetType = "RTTm".getBytes();

        byte[] sourceName = nodeName.getBytes();

        byte[] destName = myNode.getName().getBytes();

        byte[] message = new byte[64000];


//      first 30 bytes
        for(int i = 0; i < packetType.length; i++) {

            message[i] = packetType[i];

        }
//      next 16 bytes (starNode name is max 16 characters)
        int index = 30;
        for(int i = 0; i < sourceName.length; i++) {

            message[index++] = sourceName[i];

        }

//      next 16 bytes (starNode name is max 16 characters)
        index = 46;
        for(int i = 0; i < destName.length; i++) {

            message[index++] = destName[i];

        }

        int timeSent = (int)System.currentTimeMillis();

        byte[] timeSentBytes = ByteBuffer.allocate(4).putInt(timeSent).array();


//      start of body
        index = 62;
        for (int i = 0; i < timeSentBytes.length; i++) {

            message[index++] = timeSentBytes[i];

        }

        return message;


    }

}
