/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.*;

import java.util.*;

/**
 * Implementation of PRoPHET router as described in
 * <I>An Efficient Routing Protocol Using the History of
 *  Delivery Predictability in Opportunistic Networks</I>
 *  implement by Jordan Vincent.
 */
public class ProphetRouterFromPaper extends ProphetRouter {
	/** previous delivery predictabilities */
	private Map<DTNHost, Double> prevPreds;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public ProphetRouterFromPaper(Settings s) {
		super(s);
		initPrevPreds();
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ProphetRouterFromPaper(ProphetRouterFromPaper r) {
		super(r);
		initPrevPreds();
	}

	/**
	 * Initializes predictability hash
	 */
	private void initPrevPreds() {
		this.prevPreds = new HashMap<DTNHost, Double>();
	}

	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) {
			super.changedConnection(con);
		}
	}

	/**
	 * Added by Jordan Vincent
	 * Updates previous delivery predictions for a host (the encounter host (other router)).
	 * @param host The host we just met
	 */
	private void updatePrevDeliveryPredFor(DTNHost host) {
		ProphetRouterFromPaper othRouter = (ProphetRouterFromPaper) host.getRouter();
		double oldValue = othRouter.getPredFor(host);
		othRouter.prevPreds.put(host, oldValue);
	}

	/**
	 * Returns the previous prediction (P) value for a host or 0 if entry for
	 * the host doesn't exist.
	 * @param host The host to look the P for
	 * @return the current P value
	 */
	public double getPrevPredFor(DTNHost host) {
		if (prevPreds.containsKey(host)) {
			return prevPreds.get(host);
		} else {
			return 0;
		}
	}

	@Override
	public void update() {
		super.update();
	}

	/**
	 * Tries to send all other messages to all connected hosts ordered by
	 * their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	@Override
	protected Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages =
				new ArrayList<Tuple<Message, Connection>>();

		Collection<Message> msgCollection = getMessageCollection();

		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			ProphetRouterFromPaper othRouter = (ProphetRouterFromPaper)other.getRouter();

			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			for (Message m : msgCollection) {
				if (m.getTo() == other) {
					messages.add(new Tuple<Message, Connection>(m,con));
					updatePrevDeliveryPredFor(other);
					continue;
				}

				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}
				tryAllMessagesToAllConnections();

				// the other node has higher probability of delivery
				// P(B, D) > P(A, D)
				double othPred = othRouter.getPredFor(m.getTo());
				double thisPred = getPredFor(m.getTo());
				if (othPred > thisPred) {
					// has predP(A, D)
					double thisPrevPred = getPrevPredFor(m.getTo());
					if (thisPrevPred != 0) {
						//P(B, D) >= preP(A, D)
						if (othPred < thisPrevPred) {
							//ide pakai continue
							continue;
						}
					}
					messages.add(new Tuple<Message, Connection>(m,con));
					updatePrevDeliveryPredFor(other);
				}
			}
		}

		if (messages.size() == 0) {
			return null;
		}
		// System.out.println(messages);
		// sort the message-connection tuples
		Collections.sort(messages, new TupleComparator());
		return tryMessagesForConnected(messages);	// try to send messages
	}

	/**
	 * Comparator for Message-Connection-Tuples that orders the tuples by
	 * their delivery probability by the host on the other side of the
	 * connection (GRTRMax)
	 */
	private class TupleComparator implements Comparator
			<Tuple<Message, Connection>> {

		public int compare(Tuple<Message, Connection> tuple1,
						   Tuple<Message, Connection> tuple2) {
			// delivery probability of tuple1's message with tuple1's connection
			// p1 is reference tuple2
			double p1 = ((ProphetRouterFromPaper)tuple1.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple1.getKey().getTo());
			// -"- tuple2...
			// p2 is reference tuple1
			double p2 = ((ProphetRouterFromPaper)tuple2.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple2.getKey().getTo());

			// bigger probability should come first
			if (p2-p1 == 0) {
				/* equal probabilities -> let queue mode decide */
				return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
			}
			// tuple1 is less than tuple2
			else if (p2-p1 < 0) {
				return -1;
			}
			// tuple1 is greater than tuple2
			else {
				return 1;
			}
		}
	}

	@Override
	public MessageRouter replicate() {
		ProphetRouterFromPaper r = new ProphetRouterFromPaper(this);
		return r;
	}

}
