/*
 * Author Jordan Vincent
 * Universitas Sanata Dharma
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Report for generating the total number drop message per host
 */
public class MessageDropReport extends Report implements MessageListener {
    private Map<DTNHost, Integer> droppedHosts;

    /**
     * Constructor.
     */
    public MessageDropReport() {
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.droppedHosts = new HashMap<DTNHost, Integer>();
    }


    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmupID(m.getId())) {
            return;
        }

        if (dropped) {
            this.droppedHosts.put(where, this.droppedHosts.get(where)==null ? 1 : this.droppedHosts.get(where)+1);
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
    public void done() {
//		write("Message stats for scenario " + getScenarioName() + "\nsim_time: " + format(getSimTime())  + "\n" + "\n");
//
//		for(Map.Entry<DTNHost, Integer> e : droppedHosts.entrySet()) {
//			write(e.getKey().toString() + " dropped: " + e.getValue());
//		}
        for(Map.Entry<DTNHost, Integer> e : droppedHosts.entrySet()) {
            write(e.getKey().toString() + ", " + e.getValue());
        }
        super.done();
    }

}
