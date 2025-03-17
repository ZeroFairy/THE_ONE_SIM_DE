/*
 * Author by Jordan Vincent
 * Universitas Sanata Dharma
 */

package routing;

import core.*;
import routing.community.Duration;

import java.util.*;

public class PeopleRankDERouter implements RoutingDecisionEngine{
    public static final String DAMPING_FACTOR_SETTINGS = "dampingFactor";
    public static final String TRESHOLD_SETTINGS = "threshold";
    public static final String PEOPLERANK_DE_NS = "PeopleRankDERouter";

    //Tuple <Double rankingTeman, Integer totalTeman>
    public Map<DTNHost, Tuple<Double, Integer>> PeR;    //semua yang di PeR belum pasti neighbors
    public Map<DTNHost, List<Duration>> connHistory;    //semua yang pernah ketemu
    public Map<DTNHost, Double> startConnTime;          //waktu mulai koneksi
    public Set<DTNHost> neighbors;                      //Friends
    public double dampingFactor;
    public double treshold;

    public PeopleRankDERouter(Settings s) {
        Settings prSettings = new Settings(PEOPLERANK_DE_NS);

        if (prSettings.contains(DAMPING_FACTOR_SETTINGS)) {
            this.dampingFactor = prSettings.getDouble(DAMPING_FACTOR_SETTINGS);
        } else {
            this.dampingFactor = 0.8;
        }

        if (prSettings.contains(TRESHOLD_SETTINGS)) {
            this.treshold = prSettings.getDouble(TRESHOLD_SETTINGS);
        } else {
            //BUTUH DI UBAH
            this.treshold = 100;
        }

        this.PeR = new HashMap<>();
        this.connHistory = new HashMap<DTNHost, List<Duration>>();
        this.neighbors = new HashSet<DTNHost>();
        this.startConnTime = new HashMap<DTNHost, Double>();
    }

    /**
     * Copy Constructor
     */
    protected PeopleRankDERouter(PeopleRankDERouter other) {
        this.dampingFactor = other.dampingFactor;
        this.treshold = other.treshold;

        this.PeR = new HashMap<>();
        this.connHistory = new HashMap<DTNHost, List<Duration>>();
        this.neighbors = new HashSet<DTNHost>();
        this.startConnTime = new HashMap<DTNHost, Double>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {

    }

    /**
     * Di thisHost, peer adalah yang host yang satunya lagi bagi thisHost
     * Sedangkan,
     * di peer, thisHost adalah host satunya lagi bagi peer
     */
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

        double avgInterconnectionTime = this.calcInter(history);
        if (avgInterconnectionTime > this.treshold) {
            this.neighbors.add(peer);
        } else {
            if (this.neighbors.contains(peer)) {
                this.neighbors.remove(peer);
            }
        }

        for (Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet()) {
            DTNHost pear = entry.getKey();
            PeopleRankDERouter peerDE = this.getOtherDE(pear);
            double friendRank = this.calcPeR(pear);
            int totalFriends = peerDE.connHistory.size();

            PeR.put(pear, new Tuple<>(friendRank, totalFriends));
        }
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost thisHost = con.getOtherNode(peer);
        PeopleRankDERouter peerDE = this.getOtherDE(peer);

        //Untuk conHistory
        this.startConnTime.put(peer, SimClock.getTime());
        peerDE.startConnTime.put(thisHost, SimClock.getTime());
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

        if (this.neighbors.size() > 0) {
            if (this.neighbors.contains(otherHost)) {
                if (PeROtherHost >= PeRThisHost) {
                    return true;
                }
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
        return new PeopleRankDERouter(this);
    }

    public PeopleRankDERouter getOtherDE(DTNHost peer) {
        MessageRouter peerRouter = peer.getRouter();

        return (PeopleRankDERouter) ((DecisionEngineRouter) peerRouter).getDecisionEngine();
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

    //Kalau friend != Neighbor
    public double calcPeR(DTNHost thisHost) {
        double sum = 0.0;

        for (Tuple<Double, Integer> tuple : PeR.values()) {
            if (!tuple.getKey().equals(thisHost)) {
                double friendRanking = tuple.getKey();
                int totalFriends = tuple.getValue();

                if (totalFriends > 0) {
                    sum += friendRanking / totalFriends;
                }
            }
        }

        return (1 - this.dampingFactor) + this.dampingFactor * sum;
    }

    //Kalau friend == Neighbor
//    public double calcPeR(DTNHost thisHost) {
//        double sum = 0.0;
//
//        for (DTNHost friend : neighbors) {
//            double friendRanking = this.PeR.get(friend).getKey();
//            int totalFriends = this.PeR.get(friend).getValue();
//
//            if (totalFriends > 0) {
//                sum += friendRanking / totalFriends;
//            }
//        }
//
//        return (1 - this.dampingFactor) + this.dampingFactor * sum;
//    }
}
