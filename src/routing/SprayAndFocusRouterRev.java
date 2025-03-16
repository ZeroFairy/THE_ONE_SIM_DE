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

import core.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SprayAndFocusRouterRev implements RoutingDecisionEngine {
    public static final String NROF_COPIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDFOCUS_NS = "SprayAndFocusRouterRev";
    public static final String TIMER_THRESHOLD_S = "transitivityTimerThreshold";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDFOCUS_NS + "." + "copies";

    public static final double defaultTransitivityThreshold = 60.0;
    public static final double DEFAULT_TIMEDIFF = 300;

    private int initialNrofCopies;
    private boolean isBinary;
    private double transitivityTimerThreshold;

    protected Map<DTNHost, Double> recentEncounters;

    public SprayAndFocusRouterRev(Settings s) {
        Settings snfSettings = new Settings(SPRAYANDFOCUS_NS);

        this.initialNrofCopies = snfSettings.getInt(NROF_COPIES);
        this.isBinary = snfSettings.getBoolean(BINARY_MODE);

        if (s.contains(TIMER_THRESHOLD_S)) {
            this.transitivityTimerThreshold = snfSettings.getDouble(TIMER_THRESHOLD_S);
        } else {
            this.transitivityTimerThreshold = defaultTransitivityThreshold;
        }

        this.recentEncounters = new HashMap<DTNHost, Double>();
    }

    /*
    * Copy Constructor
    */
    protected SprayAndFocusRouterRev(SprayAndFocusRouterRev snfRouterRev) {
        this.initialNrofCopies = snfRouterRev.initialNrofCopies;
        this.isBinary = snfRouterRev.isBinary;
        this.recentEncounters = new HashMap<DTNHost, Double>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {

    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {

    }

    //Belum selesai
    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        SprayAndFocusRouterRev peerHost = getOtherSNF(peer);
        DTNHost thisHost = con.getOtherNode(peer);
        double distance = thisHost.getLocation().distance(peer.getLocation());
        double thisHostSpeed = thisHost.getPath() == null ? 0 : thisHost.getPath().getSpeed();
        double peerSpeed = peer.getPath() == null ? 0 : peer.getPath().getSpeed();
        double thisHostTimeDiff = thisHostSpeed == 0.0 ? DEFAULT_TIMEDIFF : distance/thisHostSpeed;
        double peerTimeDiff = peerSpeed == 0.0 ? DEFAULT_TIMEDIFF : distance/peerSpeed;

        this.recentEncounters.put(peer, SimClock.getTime());
        peerHost.recentEncounters.put(thisHost, SimClock.getTime());

        Set<DTNHost> hostSet = new HashSet<DTNHost>(this.recentEncounters.size() + peerHost.recentEncounters.size());
        hostSet.addAll(this.recentEncounters.keySet());
        hostSet.addAll(peerHost.recentEncounters.keySet());

        for (DTNHost host : hostSet) {
            double thisTime, peerTime;

            if (this.recentEncounters.containsKey(host)) {
                thisTime = this.recentEncounters.get(host);
            } else {
                thisTime = -1;
            }

            if (peerHost.recentEncounters.containsKey(host)) {
                peerTime = this.recentEncounters.get(host);
            } else {
                peerTime = -1;
            }

            if (peerTime < 0 || thisTime + thisHostTimeDiff < peerTime) {
                this.recentEncounters.put(host, peerTime - thisHostTimeDiff);
            }

            if (thisTime < 0 || peerTime + peerTimeDiff < thisTime) {
                this.recentEncounters.put(host, thisTime - peerTimeDiff);
            }
        }
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROPERTY, this.initialNrofCopies);

//        Kalau pesan dibuat untuk diri sendiri
        return !(m.getTo() == m.getFrom());
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    //Kapan pesan butuh disimpan untuk dikirimkan ke yang lain?
    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        //Kalau host ini adalah tujuannya, maka pesan tidak dilanjutkan
        if (m.getTo() == thisHost) {
            return false;
        }

        if (isBinary) {
            int nrofCopies = (int) m.getProperty(MSG_COUNT_PROPERTY);
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
            return true;
        }

        if ((int) m.getProperty(MSG_COUNT_PROPERTY) > 1) {
            return true;
        }

        //Kalau
        if (!getOtherSNF(otherHost).recentEncounters.containsKey(m.getTo())) {
            return false; //KENAPA???
        }

        //Kalau
        if (!this.recentEncounters.containsKey(m.getTo())) {
            return true; //KENAPA???
        }

        //Kalau
        if (getOtherSNF(otherHost).recentEncounters.get(m.getTo()) > this.recentEncounters.get(m.getTo())) {
            return true; //KENAPA???
        }

        return false;
    }

    //Kapan pesan boleh di hapus???
    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {

        //Kalau pesan sudah sampai pada tujuan
        if (m.getTo() == otherHost) {
            return true;
        }

        int nrofCopies = (int) m.getProperty(MSG_COUNT_PROPERTY);

        //Kalau pesan sudah sampai di dest host
        if (nrofCopies <= 1) {
            return true; //Hapus pesan.
        }

        if (isBinary)  {
            nrofCopies = (int) Math.floor(nrofCopies/2);
            m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        } else {
            m.updateProperty(MSG_COUNT_PROPERTY, initialNrofCopies--);
        }

        return false;
    }

    //Hapus pesan kalau pesan telah ada atau pesan sudah rusak???
    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {

        //Kalau pesan sudah rusak
        return m.getTtl() <= 0 || isFinalDest(m, hostReportingOld);
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SprayAndFocusRouterRev(this);
    }

    private SprayAndFocusRouterRev getOtherSNF (DTNHost host) {
        return (SprayAndFocusRouterRev)((DecisionEngineRouter)host.getRouter()).getDecisionEngine();
    }
}
