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
 * total contact per time unit???
 */
public class Tugas1_Report_Contact extends Report implements MessageListener, UpdateListener, ConnectionListener {
    //	TotalContact, DeliveryRatio, OverheadRatio, AverageLatency, TotalForward
    private int nrofContact;
    private int contactInterval, lastUpdateContact;

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
    public Tugas1_Report_Contact() {
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
//		Haggle 1000
//		Reality 5000 atau 10000
//		Random Way Point 1000
        this.contactInterval = 1000;
        this.lastUpdateContact = 0;
        this.nrofContact = 0;
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
        super.done();
    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        if (isWarmup()) {
            return;
        }
        this.nrofContact++;
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {

    }

    @Override
    public void updated(List<DTNHost> hosts) {
        if ((this.nrofContact - this.lastUpdateContact) >= this.contactInterval) {
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

            write(this.nrofContact + "," + deliveryProb + "," + overHead + "," + avgLatency + "," + this.nrofRelayed);

            this.lastUpdateContact = this.nrofContact;
        }
    }
}
