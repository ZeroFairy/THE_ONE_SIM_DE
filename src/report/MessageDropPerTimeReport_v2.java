/*
 * Author Jordan Vincent
 * Universitas Sanata Dharma
 */
package report;

import core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Report for generating the total number drop message each host per time
 */
public class MessageDropPerTimeReport_v2 extends Report implements MessageListener, UpdateListener {
    private Map<DTNHost, List<Integer>> droppedHosts;
    private int timeInterval, lastUpdateTime, nrofupdated, nrofdropped;

    /**
     * Constructor.
     */
    public MessageDropPerTimeReport_v2() {
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.droppedHosts = new HashMap<DTNHost, List<Integer>>();
        this.timeInterval = 1800;
        this.lastUpdateTime = 0;
        this.nrofupdated = 0;

        this.fillMapWithList();
    }


    private void fillMapWithList() {
        List<DTNHost> hosts = SimScenario.getInstance().getHosts();
        for (DTNHost host : hosts) {
            List<Integer> baru = new ArrayList<Integer>();
            this.droppedHosts.put(host, baru);
//			System.out.println(host);
        }
    }


    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmupID(m.getId())) {
            return;
        }

        if (dropped) {
            this.nrofdropped++;
            List<Integer> added = this.droppedHosts.get(where);
            int prev = 0;
            try {
                prev = added.get(nrofupdated);
            } catch (IndexOutOfBoundsException e) {
                System.out.println("Catch");
                prev = 0;
            }

            if (added.size() < nrofupdated) {

            }
            added.add(nrofupdated, prev + 1);
            this.droppedHosts.put(where, added);
        }
    }


    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }


    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {
    }


    public void newMessage(Message m) {
    }


    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    }


    @Override
    public void updated(List<DTNHost> hosts) {
        if ((SimClock.getIntTime() - this.lastUpdateTime) >= this.timeInterval) {
            this.nrofupdated++;
            this.lastUpdateTime = SimClock.getIntTime();
        }
    }

    @Override
    public void done() {
        int totalAll = 0;
        write("Total Message Drop in Scenario : " + String.valueOf(this.nrofdropped));
        for(Map.Entry<DTNHost, List<Integer>> e : droppedHosts.entrySet()) {
            StringBuilder output = new StringBuilder(e.getKey().toString());
            output.append(" dropped : ");
            List<Integer> listValue = e.getValue();

            int count = 0;
            for (int i : listValue) {
                output.append(i).append(", ");
                count += i;
            }

            totalAll += count;
            write(output.toString() + "Total: " + count);
        }
        write("Total All: " + totalAll);
        super.done();
    }

//	@Override
//	public void done() {
//		for(Map.Entry<DTNHost, List<Integer>> e : droppedHosts.entrySet()) {
//			String output = e.getKey().toString() + " dropped: ";
//			List<Integer> listValue = e.getValue();
//			for (int i : listValue) {
//				output += i + ", ";
//			}
//			write(output);
//		}
//		super.done();
//	}
}