import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class StarNode{

    static Map<String, MyNode> knownNodes = new HashMap<String, MyNode>();
    static MyNode hub = null;
    static Map<String, Integer> rttVector = null;

    public static void main(String[] args) {




        try {

            String nodeName = args[0];
            String localIPAddress = InetAddress.getLocalHost().getHostAddress();
            int localPort = Integer.parseInt(args[1]);

            String pocIPAddress = InetAddress.getByName(args[2]).getHostAddress();
            int pocPort = Integer.parseInt(args[3]);

            int numberOfNodes = Integer.parseInt(args[4]);

            MyNode currentNode = new MyNode(nodeName, localIPAddress, localPort);

            knownNodes.put(nodeName, currentNode);

            DatagramSocket socket = new DatagramSocket(localPort);


            //POC Connect Thread

            //Receiving Messages Thread - Omega
            Thread receiveThread = new Thread(new ReceiveMultiThread(nodeName, socket, knownNodes, hub, rttVector));
            receiveThread.start();

            //Calculating RTT Thread - Yizra
            Thread sendRTT = new Thread(new SendRTT(nodeName, socket, knownNodes));
            sendRTT.start();

            //Sending content Thread

            Thread sendContent = new Thread(new SendContent(nodeName, socket, knownNodes, hub, rttVector));
            sendContent.start();


        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        } catch (SocketException e) {
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