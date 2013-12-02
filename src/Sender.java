import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Sender {

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;

    Window window;

    int seqNo = 0;

    public static void main(String[] args) {
        if (args.length != 5) {
            for (String arg : args)
                System.out.println(arg);
            System.out.println("Sender Usage: ftp_client server-host-name server-port# file-name N MSS");
            return;
        }

        int mss = Integer.parseInt(args[4]);
        int N = Integer.parseInt(args[3]);
        Sender sender = new Sender(args[0], Integer.parseInt(args[1]), N);

        sender.sendInt(mss);
        sender.sendInt(N);
        long startTime = System.currentTimeMillis();
        sender.sendFile(args[2], N, mss);
        long endTime = System.currentTimeMillis();
        System.out.println("Time taken = " + (endTime-startTime)/1000);
        sender.socket.disconnect();
        sender.socket.close();

    }

    private byte[] readFileFromIndex(byte[] file, int index, int size) {
        int j;
        byte array[] = new byte[size];
        for (j = 0; j < size; j++) {
            if (index >= file.length) {
                if(j==0)
                    return null;
                byte[] smaller = new byte[j];
                System.arraycopy(array, 0, smaller, 0, j);
                return smaller;
            }
            array[j] = file[index++];
        }
        return array;
    }

    private void sendFile(String fileName, int N, int mss) {
        try {
            File file = new File(fileName);
            byte[] fileInBytes = new byte[(int) file.length()];

            FileInputStream fin = new FileInputStream(file);
            BufferedInputStream bin = new BufferedInputStream(fin);
            bin.read(fileInBytes, 0, fileInBytes.length);

            byte[] fileSize = intToBytes(fileInBytes.length);
            socket.send(new DatagramPacket(fileSize, fileSize.length, serverAddress, serverPort));

            int fileIndex = 0;
            byte[] array;
            for (int i = 0; i < window.maxSize; i++) {
                array = readFileFromIndex(fileInBytes, fileIndex, mss);
                if(array != null) {
                    Packet packet = new Packet(array, seqNo++);
                    window.packetList[window.windowSize++] = packet;
                    sendPacket(packet);
                    fileIndex += array.length;
                }
            }

            byte[] ack = new byte[4];
            DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, serverAddress, serverPort);

            while (window.windowSize != 0) {
                try {
                    socket.receive(ackPacket);

                    int ackedSeqNo = bytesToInt(ackPacket.getData());
                    window.markAcknowledgedPacket(ackedSeqNo);

                    if(window.allPacketsAcknowledged()) {
                        fileIndex = slideWindow(fileInBytes, fileIndex, mss);
                    }
                }
                catch(SocketTimeoutException e) {
                    Packet lostPacket = window.getFirstUnackedPacket();
                    System.out.println("Timeout, sequence number = " + lostPacket.seqNo);
                    sendPacket(lostPacket);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int slideWindow(byte[] fileInBytes, int fileIndex, int mss) throws IOException {
        window.clear();
        for(int i=0; i< window.maxSize; i++) {
            byte[] data = readFileFromIndex(fileInBytes, fileIndex, mss);
            if(data != null) {
                fileIndex += data.length;
                Packet packet = new Packet(data, seqNo++);
                window.packetList[window.windowSize++] = packet;
                sendPacket(packet);
            }
        }
        return fileIndex;
    }

    private void sendPacket(Packet packet) throws IOException {
        byte[] buf = packet.dataWithSeqNo();
        DatagramPacket dgram = new DatagramPacket(buf, buf.length, serverAddress, serverPort);
        socket.send(dgram);
        socket.setSoTimeout(1000);
    }

    private void sendInt(int mss) {
        try {
            byte[] buff = intToBytes(mss);
            DatagramPacket packet = new DatagramPacket(buff, buff.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    public Sender(String serverName, int port, int windowSize) {
        try {
            this.socket = new DatagramSocket();
            this.serverAddress = InetAddress.getByName(serverName);
            this.serverPort = port;
            this.window = new Window(windowSize);

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static byte[] intToBytes(int n) {
        return (ByteBuffer.allocate(Integer.SIZE).order(ByteOrder.BIG_ENDIAN).putInt(n)).array();
    }

    private static int bytesToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

}
