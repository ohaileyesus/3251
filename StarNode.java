public class StarNode{
    public Set<MyNode> knownNodes = new Set<MyNode>();
    MyNode hub;

    MyNode currentNode = //new MyNode(....);
    MyNode pocNode = //new MyNode(....);
    knownNodes.add(currentNode, pocNode);

    DatagramSocket serverSocket = new DatagramSocket(9999);



    //POC Connect Thread

    //Receiving Messages Thread - Omega
    Thread receiveThread = new Thread(new ReceiveMultiThread(socket, knownNodes));
    receiveThread.start();

    //Calculating RTT Thread - Yizra
    
    //Sending content Thread

}