import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Set;

public class SendRTT implements  Runnable{


    private Set<MyNode> knownNodes;
    DatagramSocket socket;


    public SendRTT(DatagramSocket socket, Set<MyNode> knownNodes) {
        this.socket = socket;
        this.knownNodes = knownNodes;
    }

    public void run() {


        try {

            while (true) {

                for (MyNode myNode : knownNodes) {

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

        byte[] sourceIP = socket.getLocalAddress().getHostAddress().getBytes();

        byte sourcePort = (byte)socket.getLocalPort();

        byte[] destIP = myNode.getIP().getBytes();

        byte destPort = (byte)myNode.getPort();

        byte[] message = new byte[64000];


//      first 30 bytes
        for(int i = 0; i < packetType.length; i++) {

            message[i] = packetType[i];

        }
//      second 30 bytes
        int index = 30;
        for(int i = 0; i < sourceIP.length; i++) {

            message[index++] = sourceIP[i];

        }

//      third 30 bytes
        message[60] = sourcePort;

//      fourth 30 bytes
        index = 90;
        for(int i = 0; i < destIP.length; i++) {

            message[index++] = destIP[i];

        }

//      fifth 30 bytes
        message[120] = destPort;

        int timeSent = (int)System.currentTimeMillis();

        byte[] timeSentBytes = ByteBuffer.allocate(4).putInt(timeSent).array();

//        to decode
//        int timeReceived = ByteBuffer.wrap(bytes).getInt();


//      start of body
        index = 150;
        for (int i = 0; i < timeSentBytes.length; i++) {

            message[index++] = timeSentBytes[i];

        }

        return message;


    }

}
