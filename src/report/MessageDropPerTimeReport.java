/*
 * Author Jordan Vincent
 * Universitas Sanata Dharma
 */
package report;

import core.*;

import java.util.*;

/**
 * Report for generating the total number drop message each host per time unit
 */
public class MessageDropPerTimeReport extends Report implements MessageListener, UpdateListener {
    private Map<DTNHost, List<Integer>> droppedHosts;
    private Map<DTNHost, Integer> dropPerTimeHosts;
    private int timeInterval, lastUpdateTime, nrofdropped;

    /**
     * Constructor.
     */
    public MessageDropPerTimeReport() {
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.droppedHosts = new HashMap<DTNHost, List<Integer>>();
        this.dropPerTimeHosts = new HashMap<DTNHost, Integer>();
        this.timeInterval = 1800;
        this.lastUpdateTime = 0;

        this.fillMapWithList();
        this.fillMap();
    }


    private void fillMapWithList() {
        List<DTNHost> hosts = SimScenario.getInstance().getHosts();
        for (DTNHost host : hosts) {
            List<Integer> baru = new ArrayList<Integer>();
            this.droppedHosts.put(host, baru);
//			System.out.println(host);
        }
    }


    private void fillMap() {
        List<DTNHost> hosts = SimScenario.getInstance().getHosts();
        for (DTNHost host : hosts) {
            this.dropPerTimeHosts.put(host, 0);
//			System.out.println(host);
        }
    }


    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmupID(m.getId())) {
            return;
        }

        if (dropped) {
            this.nrofdropped++;
            this.dropPerTimeHosts.put(where, this.dropPerTimeHosts.get(where)==null ? 1 : this.dropPerTimeHosts.get(where)+1);
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
//			System.out.println("Masuk if Updated");
            for (DTNHost host : hosts) {
//				System.out.println("HdU (time): " +host+ " , GET -> " +this.dropPerTimeHosts.get(host));
//				System.out.println("HdU (host): " +host+ " , GET -> " +this.droppedHosts.get(host));

                List<Integer> valuePerInterval = this.droppedHosts.get(host);
                valuePerInterval.add(this.dropPerTimeHosts.get(host));
                this.droppedHosts.put(host, valuePerInterval);
//				System.out.println(this.droppedHosts.get(host));
            }
            this.dropPerTimeHosts.clear();
            this.fillMap();
            this.lastUpdateTime = SimClock.getIntTime();
        }
    }

    @Override
    public void done() {
        for (DTNHost host : this.droppedHosts.keySet()) {
            List<Integer> valuePerInterval = this.droppedHosts.get(host);
            valuePerInterval.add(this.dropPerTimeHosts.get(host));
            this.droppedHosts.put(host, valuePerInterval);
        }

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