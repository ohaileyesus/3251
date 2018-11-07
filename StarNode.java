import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class StarNode{


    public static void main(String[] args) {

        Map<String, MyNode> knownNodes = new HashMap<String, MyNode>();
        MyNode hub = null;

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
            Thread receiveThread = new Thread(new ReceiveMultiThread(nodeName, socket, knownNodes));
            receiveThread.start();

            //Calculating RTT Thread - Yizra
            Thread sendRTT = new Thread(new SendRTT(nodeName, socket, knownNodes));
            sendRTT.start();

            //Sending content Thread

        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        } catch (SocketException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }




    }



}