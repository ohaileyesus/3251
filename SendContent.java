import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Scanner;

public class SendContent implements Runnable{

    private String thisNode;
    private Map<String, MyNode> knownNodes;
    DatagramSocket socket;
    private MyNode hub;

    private Map<String, Integer> rttVector;


    public SendContent(String thisNode, DatagramSocket socket, Map<String, MyNode> knownNodes, MyNode hub,
                       Map<String, Integer> rttVector) {
        this.thisNode = thisNode;
        this.socket = socket;
        this.knownNodes = knownNodes;
        this.hub = hub;
        this.rttVector = rttVector;
    }

    public void run() {


        try {

            while(true) {


                System.out.println("enter command");

                Scanner scanner = new Scanner(System.in);

                String request = scanner.nextLine();

                if (request.contains("send")) {

                    byte[] message = prepareHeader(thisNode, hub.getName(), "CM");

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
