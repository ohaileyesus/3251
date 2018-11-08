import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class StarNode{

    static Map<String, MyNode> knownNodes = new HashMap<String, MyNode>();
    static MyNode hub = null;
    static Map<String, Integer> rttVector = null;
    static ArrayList<String> eventLog = null;

    public static void main(String[] args) throws Exception{

        try {
//          read in command line arguments
            String nodeName = args[0];
            int localPort = Integer.parseInt(args[1]);
            String pocIPAddress = InetAddress.getByName(args[2]).getHostAddress();
            int pocPort = Integer.parseInt(args[3]);
            int numberOfNodes = Integer.parseInt(args[4]);

//          make currentNode and put in knownNodes map
            String localIPAddress = InetAddress.getLocalHost().getHostAddress();
            MyNode currentNode = new MyNode(nodeName, localIPAddress, localPort);
            knownNodes.put(nodeName, currentNode);

            DatagramSocket socket = new DatagramSocket(localPort);

            //POC Connect Thread
            connectToPOC(currentNode, pocIPAddress, pocPort, socket);

            //Receiving Messages Thread - Omega
            Thread receiveThread = new Thread(new ReceiveMultiThread(nodeName, socket, knownNodes, hub, rttVector, eventLog));
            receiveThread.start();

            //Calculating RTT Thread - Yizra
            Thread sendRTT = new Thread(new SendRTT(nodeName, socket, knownNodes, eventLog));
            sendRTT.start();

            //Sending content Thread
            Thread sendContent = new Thread(new SendContent(nodeName, socket, knownNodes, hub, rttVector, eventLog));
            sendContent.start();

        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        } catch (SocketException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    public static void connectToPOC(MyNode thisNode, String pocIP, int pocPort, DatagramSocket socket) throws Exception{

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
                    throw new Exception("POC did not come alive in time");
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