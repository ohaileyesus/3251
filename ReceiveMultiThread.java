import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class ReceiveMultiThread implements Runnable {

    private String nodeName;
    private Map<String, MyNode> knownNodes;
    private DatagramSocket socket;

    private Map<String, Integer> rttSums;

    private int rttCount = 0;
    private int rttSum = 0;

    public ReceiveMultiThread(String nodeName, DatagramSocket socket, Map<String, MyNode> knownNodes) {
        this.nodeName = nodeName;
        this.socket = socket;
        this.knownNodes = knownNodes;
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
                        Set<MyNode> nodesToAppend = (Set<MyNode>) is.readObject();
                        int sizeBefore = knownNodes.size();
                        knownNodes.add(nodesToAppend);
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

                    rttSum += rtt;

                    rttCount++;

//                  if rtt is received from every node in knownNodes list minus itself
                    if (rttCount == knownNodes.size() - 1) {

//                      Send rttSum to all nodes
                        for (String name : knownNodes.keySet()) {

                            if (!name.equals(nodeName)) {

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

                        rttCount = 0;
                        rttSum = 0;
                    }

//              RTTs = RTT Sum Packet
                } else if (msgType.equals("RTTs")) {

                    String name = new String(Arrays.copyOfRange(receivedData, 30, 46));

                    int sum = ByteBuffer.wrap(Arrays.copyOfRange(receivedData, 46, 50)).getInt();

                    if(!name.equals(nodeName)) {
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
                }
            }

        } catch (Exception e) {

        }
    }










    public byte[] prepareHeader(MyNode myNode, String msgtype) {

        byte[] packetType = msgtype.getBytes();

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


        return message;
    }

}