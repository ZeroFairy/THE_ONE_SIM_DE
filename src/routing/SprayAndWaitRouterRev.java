/*
 * Added by Jordan Vincent
 *
 * Host yang kirim mendapatkan jumlah pesan floor
 * Host yang terima mendapatkan jumlah pesan ceil
 *
 * contoh: nrofCopies = 5
 * Host yang kirim = 2
 * Host yang terima = 3
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;

public class SprayAndWaitRouterRev implements RoutingDecisionEngine {
    public static final String NRPF_CPOIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouterRev";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "." + "copies";

    private int initialNrofCopies;
    private boolean isBinary;

    public SprayAndWaitRouterRev(Settings s) {
        Settings snwSettings = new Settings(SPRAYANDWAIT_NS);

        this.initialNrofCopies = snwSettings.getInt(NRPF_CPOIES);
        this.isBinary = snwSettings.getBoolean(BINARY_MODE);
    }

    protected SprayAndWaitRouterRev(SprayAndWaitRouterRev snwRouterRev) {
        this.initialNrofCopies = snwRouterRev.initialNrofCopies;
        this.isBinary = snwRouterRev.isBinary;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {

    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {

    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROPERTY, this.initialNrofCopies);

//        Kalau pesan dibuat untuk diri sendiri
        return !(m.getTo() == m.getFrom());
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        //Temporary
//        if (m.getTo() == aHost) {
//            System.out.println("Final : " + m.getId());
//        }

        return m.getTo() == aHost;
    }

    //Kapan pesan butuh disimpan untuk dikirimkan ke yang lain?
    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        //Kalau host ini adalah tujuannya, maka pesan tidak dilanjutkan
        if (m.getTo() == thisHost) {
//            System.out.println("Should Save - False");
            return false;
        }

        if (isBinary) {
            int nrofCopies = (int) m.getProperty(MSG_COUNT_PROPERTY);
//        System.out.println("Copies - SAVE: " + nrofCopies + " -- " + m.getId());
            m.updateProperty(MSG_COUNT_PROPERTY, (int) Math.ceil(nrofCopies / 2));
        } else {
            m.updateProperty(MSG_COUNT_PROPERTY, initialNrofCopies-(--initialNrofCopies));
        }

        return true;
    }

    //Kapan pesan butuh dikirim?
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        //Kalau host selanjutnya adalah tujuan dari pesan, maka pesan dikirim
        if (m.getTo() == otherHost) {
//            System.out.println("Should Send - True");
            return true;
        }

//        if (otherHost != null) {
//            System.out.println("Ada HOST");
//        }
//        if ((int) m.getProperty(MSG_COUNT_PROPERTY) >= 1) {
//            System.out.println("TRUE");
//        }

        //Karena kalau nrofcopies sisa 1, masuk masa WAIT
        return (int) m.getProperty(MSG_COUNT_PROPERTY) > 1 && otherHost != null;
    }

    //Kapan pesan boleh di hapus???
    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
//        if (m == null) {
//            System.out.println("Message is null");
//        }
//
//        if (otherHost == null) {
//            System.out.println("otherHost is null");
//        }

        //Kalau pesan sudah sampai pada tujuan
        if (m.getTo() == otherHost) {
//            System.out.println("Should Delete - True");
//            System.out.println(m.getId());
            return true;
        }

        int nrofCopies = (int) m.getProperty(MSG_COUNT_PROPERTY);
//        System.out.println("Copies - DELETE:" + nrofCopies + " -- " + m.getId());


        //Kalau pesan sudah sampai di dest host
        if (nrofCopies <= 1) {
            return true; //Hapus pesan.
        }

//        if ()
        if (isBinary)  {
            nrofCopies = (int) Math.floor(nrofCopies/2);
            m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);

//            return false;
        } else {
            m.updateProperty(MSG_COUNT_PROPERTY, initialNrofCopies--);
        }

        return false;
    }

    //Hapus pesan kalau pesan telah ada atau pesan sudah rusak???
    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
//        if (m.getTtl() < 0) {
//            System.out.println("Pesan MATI");
//        }

        //Kalau pesan sudah rusak
        return m.getTtl() <= 0 || isFinalDest(m, hostReportingOld);
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SprayAndWaitRouterRev(this);
    }
}
