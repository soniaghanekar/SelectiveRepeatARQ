import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Packet {
    byte[] data;
    int seqNo;
    boolean acknowledged;

    public Packet(byte[] data, int seqNo) {
        this.data = data;
        this.seqNo = seqNo;
        this.acknowledged = false;
    }

    byte[] dataWithSeqNo() throws IOException {
        byte[] seqNum = intToBytes(seqNo);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(seqNum);
        os.write(data);
        byte[] dataWithSeq = os.toByteArray();
        return dataWithSeq;
    }

    public static Packet extractPacket(byte[] byteArray) {
        byte[] seqByte = new byte[Integer.SIZE];
        System.arraycopy(byteArray, 0, seqByte, 0, Integer.SIZE);
        int seqNo = bytesToInt(seqByte);
        byte[] data = new byte[byteArray.length - Integer.SIZE];
        System.arraycopy(byteArray, Integer.SIZE, data, 0, byteArray.length - Integer.SIZE);
        return (new Packet(data, seqNo));
    }

    private static byte[] intToBytes(int n) {
        return (ByteBuffer.allocate(Integer.SIZE).order(ByteOrder.BIG_ENDIAN).putInt(n)).array();
    }

    private static int bytesToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

}
