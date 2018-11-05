public class StarNode{
    Set<MyNode> knownNodes = new Set<MyNode>();
    MyNode hub;

    MyNode currentNode = //new MyNode(....);
    MyNode pocNode = //new MyNode(....);
    knownNodes.add(currentNode, pocNode);


    //POC Connect Thread

    //Receiving Messages Thread - Omega
    Thread receiveThread = new Thread(new ReceiveMultiThread()); 
    receiveThread.start();

    //Calculating RTT Thread - Yizra
    
    //Sending content Thread

}