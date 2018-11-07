import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

public class StarNode{
    public Map<String, MyNode> knownNodes = new HashMap<String, MyNode>();
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