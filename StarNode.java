import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

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


            while(true) {


                System.out.println("enter command");

                Scanner scanner = new Scanner(System.in);

                String request = scanner.nextLine();

                if (request.contains("send")) {

                    byte[] message = prepareHeader(nodeName, hub.getName(), "CM");

//                  Put text in body of packet
                    byte[] text = request.substring(5, request.length()).getBytes();
                    int index = 46;
                    for (int i = 0; i < text.length; i++) {
                        message[index++] = text[i];
                    }

                    InetAddress ipAddress = InetAddress.getByAddress(hub.getIP().getBytes());

                    DatagramPacket sendPacket = new DatagramPacket(message, message.length, ipAddress, hub.getPort());

                    socket.send(sendPacket);

                } else if (request.contains("show-status")) {

                } else if (request.contains("show-log")) {

                } else if (request.contains("disconnect")) {

                }




            }

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