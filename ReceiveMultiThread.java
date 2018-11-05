public class ReceiveMultiThread implements Runnable {
    public void run() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(9999);
            DatagramPacket sendPacket;

            while (true) {
                byte[] message1 = new byte[64000];
                DatagramPacket receivePacket = new DatagramPacket(message1, message1.length);
                serverSocket.receive(receivePacket);

                byte[] input = receivePacket.getData();

                String headerres = new String(input); //////////////how to get just header part of msg??

                if (headerres.equals("PD")) {
                    //for (node in poc's known nodes list){
                    knownNodes.add(/*poc's known nodes from datagram*/)

                    for(MyNode neighbor: knownNodes) {
                        //pack knownNodes into proper format
                        sendPacket = new DatagramPacket();
                        serverSocket.send(sendPacket);
                    }
                } else if (headerres.equals("RTTm")) {





                    sendPacket = new DatagramPacket();
                    serverSocket.send(sendPacket);




                } else if (headerres.equals("RTTr")) {





                    sendPacket = new DatagramPacket();
                    serverSocket.send(sendPacket);




                } else if (headerres.equals("CM")) {
                    System.out.println(expressionStr);
                } else if (headerres.equals("PC")) {
                    System.out.println(expressionStr);
                }
            }

        } catch (Exception e) {

        }
    }
}