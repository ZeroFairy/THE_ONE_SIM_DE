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
import routing.community.Duration;

import java.util.*;

public class SprayAndFocusRouterRevModif implements RoutingDecisionEngine {
    public static final String NROF_COPIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDFOCUS_NS = "SprayAndFocusRouterRevModif";
    public static final String TIMER_THRESHOLD_S = "transitivityTimerThreshold";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDFOCUS_NS + "." + "copies";

    public static final double defaultTransitivityThreshold = 60.0;
    public static final double DEFAULT_TIMEDIFF = 300;

    private int initialNrofCopies;
    private boolean isBinary;
    private double transitivityTimerThreshold;
    public Map<DTNHost, Double> startConnTime;
    public Map<DTNHost, List<Duration>> connHistory;

    /**
     * Constructor
     */
    public SprayAndFocusRouterRevModif(Settings s) {
        Settings snfSettings = new Settings(SPRAYANDFOCUS_NS);

        this.initialNrofCopies = snfSettings.getInt(NROF_COPIES);
        this.isBinary = snfSettings.getBoolean(BINARY_MODE);

        if (s.contains(TIMER_THRESHOLD_S)) {
            this.transitivityTimerThreshold = snfSettings.getDouble(TIMER_THRESHOLD_S);
        } else {
            this.transitivityTimerThreshold = defaultTransitivityThreshold;
        }

        this.connHistory = new HashMap<DTNHost, List<Duration>>();
        this.startConnTime = new HashMap<DTNHost, Double>();
    }

    /**
    * Copy Constructor
    */
    protected SprayAndFocusRouterRevModif(SprayAndFocusRouterRevModif snfRouterRev) {
        this.initialNrofCopies = snfRouterRev.initialNrofCopies;
        this.isBinary = snfRouterRev.isBinary;
        this.connHistory = new HashMap<DTNHost, List<Duration>>();
        this.startConnTime = new HashMap<DTNHost, Double>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        this.startConnTime.put(peer, SimClock.getTime());
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double startTime = startConnTime.get(peer);
        double endTime = SimClock.getTime();

        //Ambil history yang ada
        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connHistory.put(peer, history);
        } else {
            history = connHistory.get(peer);
        }

        history.add(new Duration(startTime, endTime));
        connHistory.put(peer, history);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {}

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

        if ((int) m.getProperty(MSG_COUNT_PROPERTY) == 1) {
            SprayAndFocusRouterRevModif de = getOtherSNF(otherHost);
            double myHost = this.connHistory.get(m.getTo()) != null ? this.calcInter(this.connHistory.get(m.getTo())) : 0;
            double thatHost = de.connHistory.get(m.getTo()) != null ? de.calcInter(de.connHistory.get(m.getTo())) : 0;
            if (myHost > thatHost) {
                return true;
            }
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
        return new SprayAndFocusRouterRevModif(this);
    }

    private SprayAndFocusRouterRevModif getOtherSNF (DTNHost host) {
        return (SprayAndFocusRouterRevModif)((DecisionEngineRouter)host.getRouter()).getDecisionEngine();
    }

    // Calculate average interconnection time
    public double calcInter(List<Duration> history) {
        double sum = 0.0;
        int counter = 0;
        Iterator<Duration> it = history.iterator();
        if (!it.hasNext()) {
            return 0.0;
        }

        Duration d = it.next();
        Duration temp;
        while (it.hasNext()) {
            temp = d;
            d = it.next();
            sum += d.start - temp.end;
            counter++;
        }

        return sum / counter;
    }
}
