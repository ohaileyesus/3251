import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

public class SendContent implements Runnable{

    private String thisNode;
    private Map<String, MyNode> knownNodes;
    DatagramSocket socket;
    private MyNode hub;
    private Map<String, Integer> rttVector;
    private ArrayList<String> eventLog;
    private int maxNodes;



    public SendContent(String thisNode, DatagramSocket socket, Map<String, MyNode> knownNodes, MyNode hub,
                       Map<String, Integer> rttVector, ArrayList<String> eventLog, int maxNodes) {
        this.thisNode = thisNode;
        this.socket = socket;
        this.knownNodes = knownNodes;
        this.hub = hub;
        this.rttVector = rttVector;
        this.eventLog = eventLog;
        this.maxNodes = maxNodes;
    }

    public void run() {

        while(true) {
            if(knownNodes.size() == maxNodes) {
                try {
                    while (true) {
                        System.out.println("enter command");
                        Scanner scanner = new Scanner(System.in);
                        String request = scanner.nextLine();
                        if (request.contains("send")) {

                            //if ASCII message
                            if (request.contains("\"")) {
                                byte[] message = prepareHeader(thisNode, hub.getName(), "CMA");

                                //Put text in body of packet
                                byte[] text = request.substring(5, request.length()).getBytes();
                                //format of packet = 62 header bytes + 1 byte for text length + the body of the text
                                message[62] = (byte) text.length;
                                int index = 63;
                                for (int i = 0; i < text.length; i++) {
                                    message[index++] = text[i];
                                }
                                byte[] ipAsByteArr = convertIPtoByteArr(hub.getIP());
                                InetAddress ipAddress = InetAddress.getByAddress(ipAsByteArr);
                                DatagramPacket sendPacket = new DatagramPacket(message, message.length, ipAddress, hub.getPort());
                                socket.send(sendPacket);
                            }
                            //if file message
                            else {
                                //convert file into byte array
                                String filename = request.substring(5, request.length());
                                File file = new File(filename);
                                byte[] fileAsByteArr = new byte[(int) file.length()];

                                try {
                                    //put file into fileinputstream
                                    FileInputStream fileInputStream = new FileInputStream(file);
                                    fileInputStream.read(fileAsByteArr);
                                    byte[] message = prepareHeader(thisNode, hub.getName(), "CMF");

                                    //format of packet = 62 bytes header + 1 byte filename length + filename + 1 byte file length + file

                                    //1 byte filename length
                                    message[62] = (byte) filename.length();

                                    //filename
                                    int index = 63;
                                    for (int i = 0; i < filename.length(); i++) {
                                        message[index++] = (byte) filename.charAt(i);
                                    }

                                    //1 byte file length
                                    message[index + filename.length()] = (byte) file.length();

                                    //file
                                    index = index + filename.length() + 1;
                                    for (int i = 0; i < fileAsByteArr.length; i++) {
                                        message[index++] = fileAsByteArr[i];
                                    }

                                    byte[] ipAsByteArr = convertIPtoByteArr(hub.getIP());
                                    InetAddress ipAddress = InetAddress.getByAddress(ipAsByteArr);
                                    DatagramPacket sendPacket = new DatagramPacket(message, message.length, ipAddress, hub.getPort());
                                    socket.send(sendPacket);

                                } catch (FileNotFoundException e) {
                                    System.out.println("File Not Found.");
                                    e.printStackTrace();
                                } catch (IOException e1) {
                                    System.out.println("Error Reading The File.");
                                    e1.printStackTrace();
                                }
                            }

                            eventLog.add(String.valueOf(System.currentTimeMillis()) + ": Sent message");

                        } else if (request.contains("show-status")) {

                            System.out.println("Active Nodes in the network: ");

                            for (String nodeName : rttVector.keySet()) {
                                System.out.println(nodeName + " is " + rttVector.get(nodeName) + " seconds away");
                            }
                            System.out.println("\n" + hub.getName() + " is the hub.");

                        } else if (request.contains("disconnect")) {
                            if (hub.getName().equals(thisNode)) {
                                //send Delete Hub msg
                                byte[] message = prepareHeader(thisNode, hub.getName(), "DH");
                                byte[] ipAsByteArr = convertIPtoByteArr(hub.getIP());
                                InetAddress ipAddress = InetAddress.getByAddress(ipAsByteArr);
                                DatagramPacket sendPacket = new DatagramPacket(message, message.length, ipAddress, hub.getPort());
                                socket.send(sendPacket);
                            } else {
                                //send Delete Regular msg
                                byte[] message = prepareHeader(thisNode, hub.getName(), "DR");
                                byte[] ipAsByteArr = convertIPtoByteArr(hub.getIP());
                                InetAddress ipAddress = InetAddress.getByAddress(ipAsByteArr);
                                DatagramPacket sendPacket = new DatagramPacket(message, message.length, ipAddress, hub.getPort());
                                socket.send(sendPacket);
                            }
                            eventLog.add(String.valueOf(System.currentTimeMillis()) + ": A node disconnected");

                        } else if (request.contains("show-log")) {
                            for (String event : eventLog) {
                                System.out.println(event);
                            }
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
        }





    }

    public byte[] prepareHeader(String thisNode, String destNode, String msgtype) {

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

    public byte[] convertIPtoByteArr(String ipAddress) {
        String[] ip = ipAddress.split("\\.");
        byte[] ipAsByteArr = new byte[4];
        int temp;
        for (int i = 0; i < 4; i++) {
            temp = Integer.parseInt(ip[i]);
            ipAsByteArr[i] = (byte) temp;
        }
        return ipAsByteArr;
    }
}
