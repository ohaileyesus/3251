import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

public class ConnectToPOC implements Runnable{

    private MyNode thisNode;
    private Map<String, MyNode> knownNodes;
    private String pocIP;
    private int pocPort;
    DatagramSocket socket;


    public ConnectToPOC(MyNode thisNode, Map<String, MyNode> knownNodes, String pocIP, int pocPort, DatagramSocket socket) {
        this.thisNode = thisNode;
        this.knownNodes = knownNodes;
        this.pocIP = pocIP;
        this.pocPort = pocPort;
        this.socket = socket;
    }

    public void run() {

        connectToPOC(thisNode, knownNodes, pocIP, pocPort, socket);


    }

    public static void connectToPOC(MyNode thisNode, Map<String, MyNode> knownNodes, String pocIP, int pocPort, DatagramSocket socket) {

        try {

            byte[] message = prepareHeader(thisNode.getName(), "no name", "PC");

//          Put source IP and Port in body
            byte[] sourceIP = thisNode.getIP().getBytes();
            int index = 62;
            for (int i = 0; i < sourceIP.length; i++) {
                message[index++] = sourceIP[i];
            }
            byte[] sourcePort = ByteBuffer.allocate(4).putInt(thisNode.getPort()).array();
            for (int i = 0; i < sourcePort.length; i++) {
                message[index++] = sourcePort[i];
            }

            String[] ip = pocIP.split("\\.");
            byte[] ipAsByteArr = new byte[4];
            int temp;
            for (int i = 0; i < 4; i++) {
                temp = Integer.parseInt(ip[3 - i]);
                ipAsByteArr[i] = (byte) temp;
            }
            InetAddress ipAddress = InetAddress.getByAddress(ipAsByteArr);
            DatagramPacket sendPacket = new DatagramPacket(message, message.length, ipAddress, pocPort);
            socket.setSoTimeout(5000);
            byte[] response = new byte[64000];
            DatagramPacket receivePacket = new DatagramPacket(response, response.length);


//          keep sending every 5 seconds until PCr packet received
            int sendAttempts = 0;
            while (true) {

//              there are 24 "5-sec periods" in 2 minutes, so quit at 25th send attempt
                if (sendAttempts == 25) {
                    System.exit(0);
                    System.out.println("POC did not come alive in time");
                }
                socket.send(sendPacket);

                try {
                    socket.receive(receivePacket);
                    byte[] receivedData = receivePacket.getData();
                    String msgType = new String(Arrays.copyOfRange(receivedData, 0, 30));
                    if (msgType.equals("PCr")) {
//                      Add pocNode to knownNodes map
                        String name = new String(Arrays.copyOfRange(receivedData, 30, 46));
                        MyNode pocNode = new MyNode(name, pocIP, pocPort);
                        knownNodes.put(name, pocNode);
                        System.out.println("poc connected");
                        break;
                    }
                } catch (SocketTimeoutException e) {

                }
                sendAttempts++;
            }

        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    public static byte[] prepareHeader(String thisNode, String destNode, String msgtype) {

        byte[] packetType = msgtype.getBytes();
        byte[] sourceName = thisNode.getBytes();
        byte[] destName = destNode.getBytes();
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

        return message;
    }

}
