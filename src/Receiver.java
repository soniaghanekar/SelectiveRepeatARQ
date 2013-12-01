import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.lang.Math.random;

public class Receiver {
    DatagramSocket socket;

    InetAddress clientAddress;
    int clientPort;

    Window window;

    public static void main(String[] args) {
        if(args.length != 3) {
            System.out.println("Receiver Usage: UDPServer port# file-name p");
            return;
        }

        try {
            Receiver receiver = new Receiver(Integer.parseInt(args[0]));
            receiver.receiveFile(args[1], Double.parseDouble(args[2]));

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void receiveFile(String fileName, double failureProbability) throws IOException {

        int mss = receiveInt();
        int N = receiveInt();
        int fileSize = receiveInt();

        window = new Window(N);

        byte[] bytearray = new byte[mss + Integer.SIZE];
        DatagramPacket fileBytes = new DatagramPacket(bytearray, bytearray.length);
        PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));

        int count = 0;
        int seqStart = 0;
        while(count < fileSize) {
            socket.receive(fileBytes);
            byte[] data = getProperSizedBuffer(fileBytes);
            Packet receivedPacket = Packet.extractPacket(data);

            if(random() > failureProbability) {
                if(seqStart <= receivedPacket.seqNo && receivedPacket.seqNo < (seqStart+window.maxSize)) {
                    window.packetList[receivedPacket.seqNo-seqStart] = receivedPacket;
                    window.windowSize++;
                    byte[] seqBytes = intToBytes(receivedPacket.seqNo);
                    socket.send(new DatagramPacket(seqBytes, seqBytes.length, clientAddress, clientPort));
                    count += receivedPacket.data.length;

                    if(window.windowSize == window.maxSize || count >= fileSize) {
                        for(int i=0; i<window.windowSize; i++) {
                           printWriter.print(new String(window.packetList[i].data));
                        }
                        window.clear();
                        seqStart += window.maxSize;
                    }
                    fileBytes = new DatagramPacket(bytearray, bytearray.length);
                }
                else if(window.packetExistsInWindowWithAck(receivedPacket.seqNo)) {
                    byte[] seqBytes = intToBytes(receivedPacket.seqNo);
                    socket.send(new DatagramPacket(seqBytes, seqBytes.length, clientAddress, clientPort));
                }
            }
            else {
                System.out.println("Packet loss, sequence number = " + receivedPacket.seqNo);
            }
        }
        printWriter.close();
        socket.close();

    }

    public Receiver(int port) {
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static byte[] getProperSizedBuffer(DatagramPacket fileBytes) {
        if(fileBytes.getLength() != fileBytes.getData().length){
            byte[] bytes = new byte[fileBytes.getLength()];
            System.arraycopy(fileBytes.getData(), 0, bytes, 0, fileBytes.getLength());
            return bytes;
        }
        return fileBytes.getData();
    }

    private int receiveInt() throws IOException {
        byte[] receiveData = new byte[Integer.SIZE];
        DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(packet);
        if(clientAddress == null)
            setClientInfo(packet);
        return bytesToInt(receiveData);
    }

    private void setClientInfo(DatagramPacket packet) {
        clientAddress = packet.getAddress();
        clientPort = packet.getPort();
    }

    private static byte[] intToBytes(int n) {
        return (ByteBuffer.allocate(Integer.SIZE).order(ByteOrder.BIG_ENDIAN).putInt(n)).array();
    }

    private static int bytesToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

}
