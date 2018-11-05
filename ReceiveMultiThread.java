public class ReceiveMultiThread implements Runnable {

    private Set<MyNode> knownNodes;
    DatagramSocket socket;

    public ReceiveMultiThread(DatagramSocket socket, Set<MyNode> knownNodes) {
        this.socket = socket;
        this.knownNodes = knownNodes;
    }

    public void run() {
        try {

            while (true) {

                //receive packet
                byte[] message1 = new byte[64000];
                DatagramPacket receivePacket = new DatagramPacket(message1, message1.length);
                serverSocket.receive(receivePacket);
                byte[] receivedData = receivePacket.getData();

                String msgType = new String(Arrays.copyOfRange(receivedData, 0, 30));

                if (msgType.equals("PD")) {

                    //read knownNodes list from object input stream
                    ByteArrayInputStream in = new ByteArrayInputStream(Arrays.copyOfRange(receivedData, 150, receivedData.length - 1));
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
                    for(MyNode neighbor: knownNodes) {
                        byte[] dataToSend = preparePacket(neighbor, "PD");
                        sendPacket = new DatagramPacket(dataToSend, dataToSend.length, neighbor.ip, neighbor.port);
                        serverSocket.send(sendPacket);
                    }








                } else if (msgType.equals("RTTm")) {
                    sendPacket = new DatagramPacket();
                    serverSocket.send(sendPacket);




                } else if (msgType.equals("RTTr")) {
                    sendPacket = new DatagramPacket();
                    serverSocket.send(sendPacket);




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










    public byte[] preparePacket(MyNode myNode, String msgtype) {

        byte[] packetType = msgtype.getBytes();

        byte[] sourceIP = socket.getLocalAddress().getHostAddress().getBytes();

        byte[] sourcePort = ByteBuffer.allocate(4).putInt(socket.getLocalPort()).array();

        byte[] destIP = myNode.getIP().getBytes();

        byte[] destPort = ByteBuffer.allocate(4).putInt(myNode.getPort()).array();

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
        int index = 60;
        for(int i = 0; i < sourcePort.length; i++) {

            message[index++] = sourcePort[i];

        }

//      fourth 30 bytes
        index = 90;
        for(int i = 0; i < destIP.length; i++) {

            message[index++] = destIP[i];

        }

//      fifth 30 bytes
        index = 120;
        for(int i = 0; i < destPort.length; i++) {

            message[index++] = destPort[i];

        }

        int timeSent = (int)System.currentTimeMillis();

        byte[] timeSentBytes = ByteBuffer.allocate(4).putInt(timeSent).array();

//      start of body
        index = 150;
        for (int i = 0; i < timeSentBytes.length; i++) {

            message[index++] = timeSentBytes[i];

        }

        return message;
    }

}