import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ReceiveMultiThread implements Runnable {

    private String thisNode;
    private Map<String, MyNode> knownNodes;
    private DatagramSocket socket;
    private MyNode hub;

    private Map<String, Integer> rttVector;
    private Map<String, Integer> rttSums;

    private int rttSum = 0;

    public ReceiveMultiThread(String thisNode, DatagramSocket socket, Map<String, MyNode> knownNodes, MyNode hub,
                              Map<String, Integer> rttVector) {
        this.thisNode = thisNode;
        this.socket = socket;
        this.knownNodes = knownNodes;
        this.hub = hub;
        this.rttVector = rttVector;
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
                        for (String nameOfNodeToAppend: nodesToAppend.keyset()) {
                            if (!nodesToAppend.containsKey(nameOfNodeToAppend)) {
                                knownNodes.put(nameOfNodeToAppend, nodesToAppend.get(nameOfNodeToAppend))
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
                        byte[] dataToSend = prepareHeader(neighbor, "PD");
                        InetAddress ipAddress = InetAddress.getByAddress(neighbor.getIP().getBytes());
                        DatagramPacket sendPacket = new DatagramPacket(dataToSend, dataToSend.length, ipAddress, neighbor.getPort());
                        socket.send(sendPacket);
                    }


                } else if (msgType.equals("RTTm")) {

//                  change RTTm to RTTr
                    receivedData[3] = 'r';

//                  read star node name from messageBytes to get IP and port
                    String name = new String(Arrays.copyOfRange(receivedData, 30, 46));
                    InetAddress ipAddress = InetAddress.getByAddress(knownNodes.get(name).getIP().getBytes());
                    int port = knownNodes.get(name).getPort();

                    DatagramPacket sendPacket = new DatagramPacket(receivedData, receivedData.length, ipAddress, port);
                    socket.send(sendPacket);


                } else if (msgType.equals("RTTr")) {

                    int timeReceived = (int) System.currentTimeMillis();

                    int timeSent = ByteBuffer.wrap(Arrays.copyOfRange(receivedData, 46, 50)).getInt();

                    int rtt = timeReceived - timeSent;

                    rttVector.put(thisNode, rtt);

                    rttSum += rtt;

//                  if rtt is received from every node in knownNodes list minus itself
                    if (rttVector.size() == knownNodes.size() - 1) {

//                      Send rttSum to all nodes
                        for (String name : knownNodes.keySet()) {

                            if (!name.equals(thisNode)) {

                                MyNode node = knownNodes.get(name);

                                InetAddress ipAddress = InetAddress.getByAddress(node.getIP().getBytes());

                                byte[] message = prepareHeader(node, "RTTs");

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
                        rttSum = 0;
                    }

//              RTTs = RTT Sum Packet
                } else if (msgType.equals("RTTs")) {

                    String name = new String(Arrays.copyOfRange(receivedData, 30, 46));

                    int sum = ByteBuffer.wrap(Arrays.copyOfRange(receivedData, 46, 50)).getInt();

                    if(!name.equals(thisNode)) {
                        rttSums.put(name, sum);
                    }

                } else if (msgType.equals("CM")) {
                    ByteArrayInputStream in = new ByteArrayInputStream(Arrays.copyOfRange(receivedData, 150, receivedData.length - 1));
                    ObjectInputStream is = new ObjectInputStream(in);
                    try {
                        System.out.println(is.readObject());
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }



                } else if (msgType.equals("PC")) {
                    System.out.println(expressionStr);
                } else if (msgType.equals("DH")) {
                    String nodeToDelete = new String(Arrays.copyOfRange(receivedData, 30, 46));
                    knownNodes.remove(nodeToDelete);
                    rttVector.remove(nodeToDelete);
                    rttSums.remove(nodeToDelete);

                    //recalculate RTT by sending RTTm msg to all knownNodes
                    for (String name : knownNodes.keySet()) {
                        MyNode myNode = knownNodes.get(name);
                        InetAddress ipAddress = InetAddress.getByAddress(myNode.getIP().getBytes());
                        byte[] message = preparePacket(myNode, 'RTTm');
                        DatagramPacket sendPacket = new DatagramPacket(message, message.length, ipAddress, myNode.getPort());
                        socket.send(sendPacket);
                    }

                } else if (msgType.equals("DR")) {
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










    public byte[] prepareHeader(MyNode myNode, String msgtype) {

        byte[] packetType = msgtype.getBytes();

        byte[] sourceName = thisNode.getBytes();

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


        return message;
    }

}