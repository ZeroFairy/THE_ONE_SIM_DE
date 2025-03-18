/*
 * Author by Jordan Vincent
 * Universitas Sanata Dharma
 */

package routing;

import core.*;
import routing.community.Duration;

import java.util.*;

public class PeopleRankDERouter_v2 implements RoutingDecisionEngine{
    public static final String DAMPING_FACTOR_SETTINGS = "dampingFactor";
    public static final String TRESHOLD_SETTINGS = "treshold";
    public static final String PEOPLERANK_DE_NS = "PeopleRankDERouter_v2";
    public static final String DICIDER = "dicider";

    public Map<DTNHost, List<Duration>> connHistory;    //semua yang pernah ketemu
    public Map<DTNHost, Double> startConnTime;          //waktu mulai koneksi
    private Set<DTNHost> neighbors;                      //Friends
    private double dampingFactor;
    private double treshold;
    public double PeR;
    private Dicider dicider;

    private enum Dicider {
        INTERCONNECTION,
        CONNECTION_DURATION,
        CONNECTION_TIME;
    }

    public PeopleRankDERouter_v2(Settings s) {
        Settings prSettings = new Settings(PEOPLERANK_DE_NS);

        if (prSettings.contains(DAMPING_FACTOR_SETTINGS)) {
            this.dampingFactor = prSettings.getDouble(DAMPING_FACTOR_SETTINGS);
        } else {
            this.dampingFactor = 0.8;
        }

        this.dicider = Dicider.valueOf(prSettings.getSetting(DICIDER));
        if (prSettings.contains(TRESHOLD_SETTINGS)) {
            this.treshold = prSettings.getDouble(TRESHOLD_SETTINGS);
        } else {
            switch (dicider) {
                case INTERCONNECTION:
                    //rata2 tidak ketemu 400 second
                    this.treshold = 400;
                    break;
                case CONNECTION_DURATION:
                    //sudah ketemu lebih dari 700 second
                    this.treshold = 700;
                    break;
                case CONNECTION_TIME: //Frequent
                    //sudah ketemua lebih dari 5 kali
                    this.treshold = 5;
                    break;
            }
        }

        this.connHistory = new HashMap<DTNHost, List<Duration>>();
        this.neighbors = new HashSet<DTNHost>();
        this.startConnTime = new HashMap<DTNHost, Double>();
    }

    /**
     * Copy Constructor
     */
    protected PeopleRankDERouter_v2(PeopleRankDERouter_v2 other) {
        this.dampingFactor = other.dampingFactor;
        this.treshold = other.treshold;

        this.connHistory = new HashMap<DTNHost, List<Duration>>();
        this.neighbors = new HashSet<DTNHost>();
        this.startConnTime = new HashMap<DTNHost, Double>();
        this.dicider = other.dicider;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        //Kalau ketemu langsung hitung dulu
//        this.PeR = calcPeR(thisHost);
        this.startConnTime.put(peer, SimClock.getTime());
    }

    /**
     * Di thisHost, peer adalah yang host yang satunya lagi bagi thisHost
     * Sedangkan,
     * di peer, thisHost adalah host satunya lagi bagi peer
     */
    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
//        System.out.println("MASUK CONN" +startConnTime.toString());
//        System.out.println("PEER : " +peer);
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

        switch (dicider) {
            case INTERCONNECTION:
                double avgInterconnectionTime = this.calcInter(history);
                if (avgInterconnectionTime < this.treshold) {
                    this.neighbors.add(peer);
                } else {
                    if (this.neighbors.contains(peer)) {
                        this.neighbors.remove(peer);
                    }
                }
                break;
            case CONNECTION_DURATION:
                double dur = this.calcDuration(connHistory.get(peer));
                if (dur >= this.treshold) {
                    this.neighbors.add(peer);
                } else {
                    if (this.neighbors.contains(peer)) {
                        this.neighbors.remove(peer);
                    }
                }
                break;
            case CONNECTION_TIME:
                List<Duration> connectionTimeHistory = connHistory.get(peer);
                if (connectionTimeHistory.size() >= this.treshold) {
                    this.neighbors.add(peer);
                } else {
                    if (this.neighbors.contains(peer)) {
                        this.neighbors.remove(peer);
                    }
                }
                break;
        }


        //Untuk setelah selesai
        this.PeR = calcPeR(thisHost);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
//        DTNHost thisHost = con.getOtherNode(peer);
//        PeopleRankDERouter_v2 peerDE = this.getOtherDE(peer);


//        System.out.println("MASUK EX: " +startConnTime.toString());
        //Untuk conHistory
//        this.startConnTime.put(peer, SimClock.getTime());
//        peerDE.startConnTime.put(thisHost, SimClock.getTime());

//        System.out.println("KELUAR EX: " +startConnTime.toString());
    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost) {
            return true;
        }

        double PeRThisHost = this.calcPeR(thisHost);
        double PeROtherHost = this.calcPeR(otherHost);

        if (this.neighbors.contains(otherHost)) {
            if (PeROtherHost >= PeRThisHost) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return true;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }

    @Override
    public void update(DTNHost thisHost) {

    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new PeopleRankDERouter_v2(this);
    }

    public PeopleRankDERouter_v2 getOtherDE(DTNHost peer) {
        MessageRouter peerRouter = peer.getRouter();

        return (PeopleRankDERouter_v2) ((DecisionEngineRouter) peerRouter).getDecisionEngine();
    }

    public double calcDuration (List<Duration> history) {
        double sum = 0;
        for (Duration d : history) {
            sum += d.end - d.start;
        }
        return sum;
    }

    public double calcInter(List<Duration> history) {
        double sum = 0.0;
        int counter = 0;
        Iterator<Duration> it = history.iterator();
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

    //Kalau friend == Neighbor
    public double calcPeR(DTNHost thisHost) {
        double sum = 0.0;

        for (DTNHost friend : neighbors) {
            PeopleRankDERouter_v2 peerDE = this.getOtherDE(friend);
            double friendRanking = peerDE.PeR;
            int totalFriends = peerDE.neighbors.size();

            if (totalFriends > 0) {
                sum += friendRanking / totalFriends;
            }
        }

        return (1 - this.dampingFactor) + this.dampingFactor * sum;
    }
}
