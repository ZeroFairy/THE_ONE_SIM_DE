/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 *
 * Edited by Jordan Vincent
 */
package report;

import core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * forget
 */
public class Tugas1_Report_Copy extends Report implements MessageListener, UpdateListener, ConnectionListener {
    //	Time, TotalContact, DeliveryRatio, OverheadRatio, AverageLatency, TotalForward
    private Map<Double, List<Double>> report;
    private List<Double> totalContact, deliveryRatio, overheadRatio, averageLatency, totalForward;
    private double contact;
    private int timeInterval, lastUpdateTime;

    private Map<String, Double> creationTimes;
    private List<Double> latencies;
    private List<Integer> hopCounts;
    private List<Double> msgBufferTime;
    private List<Double> rtt; // round trip times

    private int nrofDropped;
    private int nrofRemoved;
    private int nrofStarted;
    private int nrofAborted;
    private int nrofRelayed;  //Forward
    private int nrofCreated;
    private int nrofResponseReqCreated;
    private int nrofResponseDelivered;
    private int nrofDelivered;

    /**
     * Constructor.
     */
    public Tugas1_Report_Copy() {
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.creationTimes = new HashMap<String, Double>();
        this.latencies = new ArrayList<Double>();
        this.msgBufferTime = new ArrayList<Double>();
        this.hopCounts = new ArrayList<Integer>();
        this.rtt = new ArrayList<Double>();

        this.nrofDropped = 0;
        this.nrofRemoved = 0;
        this.nrofStarted = 0;
        this.nrofAborted = 0;
        this.nrofRelayed = 0;
        this.nrofCreated = 0;
        this.nrofResponseReqCreated = 0;
        this.nrofResponseDelivered = 0;
        this.nrofDelivered = 0;

//		Newly Added
//		300 second = 5 menit
        this.timeInterval = 300;
        this.lastUpdateTime = 0;

        this.report = new HashMap<>();
        this.totalContact = new ArrayList<>();
        this.deliveryRatio = new ArrayList<>();
        this.overheadRatio = new ArrayList<>();
        this.averageLatency = new ArrayList<>();
        this.totalForward = new ArrayList<>();
        this.contact = 0.0;
    }


    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmupID(m.getId())) {
            return;
        }

        if (dropped) {
            this.nrofDropped++;
        }
        else {
            this.nrofRemoved++;
        }

        this.msgBufferTime.add(getSimTime() - m.getReceiveTime());
    }


    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        if (isWarmupID(m.getId())) {
            return;
        }

        this.nrofAborted++;
    }


    public void messageTransferred(Message m, DTNHost from, DTNHost to,
                                   boolean finalTarget) {
        if (isWarmupID(m.getId())) {
            return;
        }

        this.nrofRelayed++;
        if (finalTarget) {
            this.latencies.add(getSimTime() -
                    this.creationTimes.get(m.getId()) );
            this.nrofDelivered++;
            this.hopCounts.add(m.getHops().size() - 1);

            if (m.isResponse()) {
                this.rtt.add(getSimTime() -	m.getRequest().getCreationTime());
                this.nrofResponseDelivered++;
            }
        }
    }


    public void newMessage(Message m) {
        if (isWarmup()) {
            addWarmupID(m.getId());
            return;
        }

        this.creationTimes.put(m.getId(), getSimTime());
        this.nrofCreated++;
        if (m.getResponseSize() > 0) {
            this.nrofResponseReqCreated++;
        }
    }


    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        if (isWarmupID(m.getId())) {
            return;
        }

        this.nrofStarted++;
    }


    @Override
    public void done() {
        for(Map.Entry<Double, List<Double>> entry : report.entrySet()) {
            StringBuilder output = new StringBuilder(entry.getKey().toString());
            output.append(",");
            output.append(entry.getValue());

            write(output.toString());
        }
        super.done();
    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        if (isWarmup()) {
            return;
        }
        this.contact++;
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {

    }

    @Override
    public void updated(List<DTNHost> hosts) {
        if ((SimClock.getIntTime() - this.lastUpdateTime) >= this.timeInterval) {
            List<Double> note = new ArrayList<>();
            double deliveryProb = 0;
            double overHead = 0;
            double avgLatency = 0;

            if (this.nrofCreated > 0) {
                deliveryProb = (1.0 * this.nrofDelivered) / this.nrofCreated;
            }
            if (this.nrofDelivered > 0) {
                overHead = (1.0 * (this.nrofRelayed - this.nrofDelivered)) /
                        this.nrofDelivered;
            }
            if (this.latencies.size() > 0) {
                avgLatency = Double.parseDouble(getAverage(this.latencies));
            }
            note.add(this.contact);
            note.add(deliveryProb);
            note.add(overHead);
            note.add(avgLatency);
            note.add(Double.valueOf(this.nrofRelayed));

            report.put(SimClock.getTime(), note);

            reset();
            this.lastUpdateTime = SimClock.getIntTime();
        }
    }

    public void reset() {
        this.latencies.clear();
        this.nrofCreated = 0;
        this.nrofDelivered = 0;
        this.nrofRelayed = 0;
        this.contact = 0;
    }
}
