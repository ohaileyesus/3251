import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;

public class ReceiveMultiThread implements Runnable {

    private String thisNode;
    private Map<String, MyNode> knownNodes;
    private DatagramSocket socket;
    private MyNode hub;
    private Map<String, Integer> rttVector;
    private Map<String, Integer> rttSums = new HashMap<>();
    private ArrayList<String> eventLog;

    private boolean notFull = true;

    public ReceiveMultiThread(String thisNode, DatagramSocket socket, Map<String, MyNode> knownNodes, MyNode hub,
                              Map<String, Integer> rttVector, ArrayList<String> eventLog) {
        this.thisNode = thisNode;
        this.socket = socket;
        this.knownNodes = knownNodes;
        this.hub = hub;
        this.rttVector = rttVector;
        this.eventLog = eventLog;
    }

    public void run() {
        try {

            while (true) {

                //receive packet
                byte[] message1 = new byte[64000];
                DatagramPacket receivePacket = new DatagramPacket(message1, message1.length);
                socket.receive(receivePacket);
                byte[] receivedData = receivePacket.getData();

                String msgType = new String(Arrays.copyOfRange(receivedData, 0, 30));

                if (msgType.equals("PD")) {
                    //read knownNodes list from object input stream
                    ByteArrayInputStream in = new ByteArrayInputStream(Arrays.copyOfRange(receivedData, 62, receivedData.length));
                    ObjectInputStream is = new ObjectInputStream(in);
                    try {
                        Map<String, MyNode> nodesToAppend = (Map<String, MyNode>) is.readObject();

                        //add unknown nodes to knownNodes map
                        int sizeBefore = knownNodes.size();
                        for (String nameOfNodeToAppend: nodesToAppend.keySet()) {
                            if (!nodesToAppend.containsKey(nameOfNodeToAppend)) {
                                knownNodes.put(nameOfNodeToAppend, nodesToAppend.get(nameOfNodeToAppend));
                                eventLog.add(String.valueOf(System.currentTimeMillis()) + ": A new node has been discovered");
                            }
                        }
                        int sizeAfter = knownNodes.size();

                        //if knownNodes was already up to date, no need to continue
                        if (sizeBefore == sizeAfter) break;

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    //pack knownNodes into proper format
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    ObjectOutputStream os = new ObjectOutputStream(outputStream);
                    os.writeObject(knownNodes);

                    //update every node with new knownNodes set
                    for (String name : knownNodes.keySet()) {
                        MyNode neighbor = knownNodes.get(name);
                        byte[] dataToSend = prepareHeader(neighbor.getName(), "PD");
                        byte[] ipAsByteArr = convertIPtoByteArr(neighbor.getIP());
                        InetAddress ipAddress = InetAddress.getByAddress(ipAsByteArr);
                        DatagramPacket sendPacket = new DatagramPacket(dataToSend, dataToSend.length, ipAddress, neighbor.getPort());
                        socket.send(sendPacket);
                    }


                } else if (msgType.equals("RTTm")) {
                    eventLog.add(String.valueOf(System.currentTimeMillis()) + ": An RTT request has been received");

//                  change RTTm to RTTr
                    receivedData[3] = 'r';

//                  read star node name from messageBytes to get IP and port
                    String name = new String(Arrays.copyOfRange(receivedData, 30, 46));
                    byte[] ipAsByteArr = convertIPtoByteArr(knownNodes.get(name).getIP());
                    InetAddress ipAddress = InetAddress.getByAddress(ipAsByteArr);
                    int port = knownNodes.get(name).getPort();

                    DatagramPacket sendPacket = new DatagramPacket(receivedData, receivedData.length, ipAddress, port);
                    socket.send(sendPacket);


                } else if (msgType.equals("RTTr")) {
                    eventLog.add(String.valueOf(System.currentTimeMillis()) + ": An RTT response has been received");

                    int timeReceived = (int) System.currentTimeMillis();
                    int timeSent = ByteBuffer.wrap(Arrays.copyOfRange(receivedData, 46, 50)).getInt();
                    int rtt = timeReceived - timeSent;
                    rttVector.put(thisNode, rtt);

//                  if rtt is received from every node in knownNodes list minus itself
                    if (rttVector.size() == knownNodes.size() - 1) {

//                      find rttSum of this node
                        int rttSum = 0;
                        for (String name : rttVector.keySet()) {
                            rttSum += rttVector.get(name);
                        }
                        rttSums.put(thisNode, rttSum);

//                      Send rttSum to all nodes
                        for (String name : knownNodes.keySet()) {
                            if (!name.equals(thisNode)) {
                                MyNode node = knownNodes.get(name);
                                byte[] ipAsByteArr = convertIPtoByteArr(node.getIP());
                                InetAddress ipAddress = InetAddress.getByAddress(ipAsByteArr);
                                byte[] message = prepareHeader(node.getName(), "RTTs");

//                              Put rttSum in body of packet
                                byte[] rttSumBytes = ByteBuffer.allocate(4).putInt(rttSum).array();
                                int index = 46;
                                for (int i = 0; i < rttSumBytes.length; i++) {
                                    message[index++] = rttSumBytes[i];
                                }
                                DatagramPacket sendPacket = new DatagramPacket(message, message.length, ipAddress, node.getPort());
                                socket.send(sendPacket);
                            }
                        }
                    }

//              RTTs = RTT Sum Packet
                } else if (msgType.equals("RTTs")) {

                    String name = new String(Arrays.copyOfRange(receivedData, 30, 46));
                    int sum = ByteBuffer.wrap(Arrays.copyOfRange(receivedData, 46, 50)).getInt();

                    if(!name.equals(thisNode)) {
                        rttSums.put(name, sum);
                    }

//                  find hub if node has N rtt sums for the first time
                    if (rttSums.size() == knownNodes.size() && notFull) {
                        notFull = false;
//                      find the node with the smallest rtt sum
                        int min = Integer.MAX_VALUE;
                        MyNode minNode = null;
                        for (String nodeName : knownNodes.keySet()) {
                            if (rttSums.get(nodeName) < min) {
                                min = rttSums.get(nodeName);
                                minNode = knownNodes.get(nodeName);
                            }
                        }
                        hub = minNode;

//                  find new hub if there has been a change to rtt sum list
                    } else if (rttSums.size() == knownNodes.size() && rttSums.containsKey(name)) {
                        int min = Integer.MAX_VALUE;
                        MyNode minNode = null;
                        for (String nodeName : knownNodes.keySet()) {
                            if (rttSums.get(nodeName) < min) {
                                min = rttSums.get(nodeName);
                                minNode = knownNodes.get(nodeName);
                            }
                        }
                        hub = minNode;
                    }

                } else if (msgType.equals("CMF")) {
                    eventLog.add(String.valueOf(System.currentTimeMillis()) + ": A file has been received");

                    String senderName = new String(Arrays.copyOfRange(receivedData, 30, 46));
                    int fileNameLength = ByteBuffer.wrap(Arrays.copyOfRange(receivedData, 62, 63)).getInt();
                    int fileContentLength = ByteBuffer.wrap(Arrays.copyOfRange(receivedData, 63, 64)).getInt();
                    int startOfContent = 64 + fileNameLength;
                    String fileName = new String(Arrays.copyOfRange(receivedData, 64, startOfContent));
                    int endOfContent = startOfContent + fileContentLength;
                    byte[] fileContent = Arrays.copyOfRange(receivedData, startOfContent, endOfContent);
                    File targetFile = new File("/" + fileName);
                    OutputStream outStream = new FileOutputStream(targetFile);
                    outStream.write(fileContent);
                    System.out.println(fileName + " file received from " + senderName);


                    //if hub, forwards message to all other nodes except sender and hub itself
                    if (thisNode.equals(hub.getName())){
                        for (String neighborName: knownNodes.keySet()) {
                            if (!neighborName.equals(hub.getName()) && !neighborName.equals(senderName)) {
                                MyNode neighbor = knownNodes.get(neighborName);
                                byte[] ipAsByteArr = convertIPtoByteArr(neighbor.getIP());
                                InetAddress ipAddress = InetAddress.getByAddress(ipAsByteArr);
                                DatagramPacket sendPacket = new DatagramPacket(receivedData, receivedData.length, ipAddress, neighbor.getPort());
                                socket.send(sendPacket);
                            }
                        }
                    }

                } else if (msgType.equals("CMA")) {
                    eventLog.add(String.valueOf(System.currentTimeMillis()) + ": An ASCII message has been received");

                    //format of packet = 62 header bytes + 1 byte for text length + the body of the text
                    String senderName = new String(Arrays.copyOfRange(receivedData, 30, 46));
                    int bodyLength = receivedData[62];
                    String asciiMessageBody = new String(Arrays.copyOfRange(receivedData, 63, 63 + bodyLength));

                    System.out.println("Node " + senderName + " says: " + asciiMessageBody);

                    //if hub, forwards message to all other nodes except sender and hub itself
                    if (thisNode.equals(hub.getName())){
                        for (String neighborName: knownNodes.keySet()) {
                            if (!neighborName.equals(hub.getName()) && !neighborName.equals(senderName)) {
                                MyNode neighbor = knownNodes.get(neighborName);
                                byte[] ipAsByteArr = convertIPtoByteArr(neighbor.getIP());
                                InetAddress ipAddress = InetAddress.getByAddress(ipAsByteArr);
                                DatagramPacket sendPacket = new DatagramPacket(receivedData, receivedData.length, ipAddress, neighbor.getPort());
                                socket.send(sendPacket);
                            }
                        }
                    }

                } else if (msgType.equals("PC")) {

//                  read source star node name from messageBytes
                    String name = new String(Arrays.copyOfRange(receivedData, 30, 46));
                    byte[] message = prepareHeader(name, "PCr");

//                  read source star node ip and port from messageBytes
                    InetAddress ipAddress = InetAddress.getByAddress(Arrays.copyOfRange(receivedData, 62, 66));
                    int port = ByteBuffer.wrap(Arrays.copyOfRange(receivedData, 66, 70)).getInt();

                    DatagramPacket sendPacket = new DatagramPacket(receivedData, receivedData.length, ipAddress, port);
                    socket.send(sendPacket);

                } else if (msgType.equals("DH")) {
                    eventLog.add(String.valueOf(System.currentTimeMillis()) + ": The hub node has disconnected");
                    String nodeToDelete = new String(Arrays.copyOfRange(receivedData, 30, 46));
                    knownNodes.remove(nodeToDelete);
                    rttVector.remove(nodeToDelete);
                    rttSums.remove(nodeToDelete);

                    //recalculate RTT by sending RTTm msg to all knownNodes
                    for (String name : knownNodes.keySet()) {
                        MyNode myNode = knownNodes.get(name);

                        byte[] ipAsByteArr = convertIPtoByteArr(myNode.getIP());
                        InetAddress ipAddress = InetAddress.getByAddress(ipAsByteArr);
                        byte[] message = prepareHeader(myNode.getName(), "RTTm");
                        DatagramPacket sendPacket = new DatagramPacket(message, message.length, ipAddress, myNode.getPort());
                        socket.send(sendPacket);
                    }

                } else if (msgType.equals("DR")) {
                    eventLog.add(String.valueOf(System.currentTimeMillis()) + ": A non-hub node has disconnected");
                    //remove from knownNodes, rttVector, and rttSums
                    String nodeToDelete = new String(Arrays.copyOfRange(receivedData, 30, 46));
                    knownNodes.remove(nodeToDelete);
                    rttVector.remove(nodeToDelete);
                    rttSums.remove(nodeToDelete);
                }
            }

        } catch (Exception e) {

        }
    }

    public byte[] prepareHeader(String destNode, String msgtype) {

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
            temp = Integer.parseInt(ip[3 - i]);
            ipAsByteArr[i] = (byte) temp;
        }
        return ipAsByteArr;
    }

}