public class Window {
    Packet[] packetList;
    int maxSize;
    int windowSize;

    public Window(int N) {
        this.packetList = new Packet[N];
        this.maxSize = N;
        this.windowSize = 0;
    }

    public boolean allPacketsAcknowledged() {
        if(getFirstUnackedPacket() == null)
            return true;
        return false;
    }

    public void markAcknowledgedPacket(int ackSeqNo) {
        for(Packet packet: packetList) {
            if(packet.seqNo == ackSeqNo) {
                packet.acknowledged = true;
                return;
            }
        }
        return;
    }

    public Packet getFirstUnackedPacket() {
        for(int i=0; i<windowSize; i++){
            if(packetList[i].data != null && packetList[i].acknowledged == false)
                return packetList[i];
        }
        return null;
    }

    public void clear() {
        packetList = new Packet[maxSize];
        windowSize = 0;
    }
}
