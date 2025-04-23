/*
 * Author Jordan Vincent
 * Universitas Sanata Dharma
 *
 */
package report;

import core.DTNHost;
import core.SimClock;
import core.SimScenario;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.community.*;

import java.util.*;

/**
 *
 */
public class GlobalPopularityDetectionReport extends Report {
    private Map<DTNHost, List<Double>> hostPopularity;
    private Map<DTNHost, Double> hostPopularityPerTime;
    private int timeInterval, lastUpdateTime;

    public GlobalPopularityDetectionReport() {
        init();
    }

    protected void init() {
        super.init();
        this.hostPopularity = new HashMap<>();
        this.hostPopularityPerTime = new HashMap<>();
        this.timeInterval = 24 * 60 * 60;
        this.lastUpdateTime = 0;
//        this.fillMapWithList();
//        this.fillMap();
    }

    // ini tidak masuk sama sekali
//    public void updated(List<DTNHost> hosts) {
//        System.out.println("Update");
//        if ((SimClock.getIntTime() - this.lastUpdateTime) >= this.timeInterval) {
////			System.out.println("Masuk if Updated");
//            for (DTNHost host : hosts) {
////				System.out.println("HdU (time): " +host+ " , GET -> " +this.dropPerTimeHosts.get(host));
////				System.out.println("HdU (host): " +host+ " , GET -> " +this.droppedHosts.get(host));
//
//                MessageRouter r = host.getRouter();
//                if (!(r instanceof DecisionEngineRouter)) {
//                    continue;
//                }
//                RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
//                BubbleRap cd = (BubbleRap) de;
//
//                hostPopularityPerTime.put(host, cd.getGlobalCentrality());
//                System.out.println(host+ "Global Centrality : " + cd.getGlobalCentrality());
//
//                List<Double> valuePerInterval = this.hostPopularity.get(host);
//                valuePerInterval.add(this.hostPopularityPerTime.get(host));
//                this.hostPopularity.put(host, valuePerInterval);
////				System.out.println(this.droppedHosts.get(host));
//            }
//            this.hostPopularityPerTime.clear();
//            this.fillMap();
//            this.lastUpdateTime = SimClock.getIntTime();
//        }
//    }
//
//    private void fillMapWithList() {
//        List<DTNHost> hosts = SimScenario.getInstance().getHosts();
//        for (DTNHost host : hosts) {
//            List<Double> baru = new LinkedList<Double>();
//            this.hostPopularity.put(host, baru);
//        }
//    }
//
//    private void fillMap() {
//        List<DTNHost> hosts = SimScenario.getInstance().getHosts();
//        for (DTNHost host : hosts) {
//            this.hostPopularityPerTime.put(host, 0.0);
//        }
//    }

//    @Override
//    public void done() {
////        for (DTNHost host : this.hostPopularity.keySet()) {
////            List<Double> valuePerInterval = this.hostPopularity.get(host);
////            valuePerInterval.add(this.hostPopularityPerTime.get(host));
////            this.hostPopularity.put(host, valuePerInterval);
////        }
//
////        int totalAll = 0;
//        write("Popularity each Host per 24 Hours : ");
//        for(Map.Entry<DTNHost, List<Double>> e : hostPopularity.entrySet()) {
//            StringBuilder output = new StringBuilder(e.getKey().toString());
//            output.append(" ;global; ");
//            List<Double> listValue = e.getValue();
//
////            int count = 0;
//            for (double i : listValue) {
//                output.append(i).append("; ");
////                count += i;
//            }
//
////            totalAll += count;
//            write(output.toString());
//        }
////        write("Total All: " + totalAll);
//        super.done();
//    }

//    @Override
//    public void done() {
//        List<DTNHost> nodes = SimScenario.getInstance().getHosts();
//        List<Set<DTNHost>> communities = new LinkedList<Set<DTNHost>>();
//        Map<Integer, List<Set<DTNHost>>> nodeCommunities = new HashMap<>(); //added
//
//        for (DTNHost h : nodes) {
//            MessageRouter r = h.getRouter();
//            if (!(r instanceof DecisionEngineRouter)) {
//                continue;
//            }
//            RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
//            if (!(de instanceof CommunityDetectionEngine)) {
//                continue;
//            }
//            CommunityDetectionEngine cd = (CommunityDetectionEngine) de;
//
//            boolean alreadyHaveCommunity = false;
//            Set<DTNHost> nodeComm = cd.getLocalCommunity();
//
//            // Test to see if another node already reported this community
//            for (Set<DTNHost> c : communities) {
//                if (c.containsAll(nodeComm) && nodeComm.containsAll(c)) {
//                    alreadyHaveCommunity = true;
//                }
//            }
//
//            if (!alreadyHaveCommunity && nodeComm.size() > 0) {
//                communities.add(nodeComm);
//            }
//
//        }
//
////         print each community and its size out to the file
//        for (Set<DTNHost> c : communities) {
//            String print = "";
//            for (DTNHost dTNHost : c) {
//                print = print + "\t" + dTNHost;
//            }
//            write(print);
//        }
//        super.done();
//    }

@Override
public void done() {
    List<DTNHost> hosts = SimScenario.getInstance().getHosts();

    write("Popularity each Host per 24 Hours : ");
    for (DTNHost h : hosts) {
        MessageRouter r = h.getRouter();
        if (!(r instanceof DecisionEngineRouter)) {
            continue;
        }
        RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
        if (!(de instanceof DistributedBubbleRap)) {
            continue;
        }
        if (!(de instanceof GetGlobalPopularity)) {
                continue;
        }
        GetGlobalPopularity cd = (GetGlobalPopularity) de;
        List<Integer> popularityPerTime = cd.getGlobalPopularity();

        //PRINT
        System.out.println(popularityPerTime);

        StringBuilder output = new StringBuilder();
        output.append(h.getAddress());
        output.append(" ; global; ");

//        Iterator<Integer> iter = popularityPerTime.iterator();
//        while (iter.hasNext()) {
//            int index = (iter.next() == null ? 0 : iter.next());
//            if (iter == null) {
//                output.append(0).append("; ");
//            } else {
//                output.append(iter.next()).append("; ");
//            }
//        }
        for(Integer i : popularityPerTime) {
            output.append(i).append("; ");
        }
        write(output.toString());
    }
    super.done();
}

}
