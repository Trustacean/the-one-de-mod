/* 
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import routing.community.Duration;

public class SnFDecisionEngineModRouter implements RoutingDecisionEngine {

    /**
     * identifier for the initial number of copies setting ({@value})
     */
    public static final String NROF_COPIES = "nrofCopies";
    /**
     * identifier for the binary-mode setting ({@value})
     */
    public static final String BINARY_MODE = "binaryMode";
    /**
     * identifier for the transitivity threshold setting ({@value})
     */
    public static final String TRANSITIVITY_THRESHOLD = "transitivityThreshold";
    /**
     * SprayAndWait router's settings name space ({@value})
     */
    public static final String SPRAYANDFOCUS_NS = "SprayAndFocusDERouter";
    /**
     * Message property key
     */
    public static final String MSG_COUNT_PROP = SPRAYANDFOCUS_NS + "." + "copies";

    /* Spray And Focus router properties */
    protected int initialNrofCopies;
    protected boolean isBinary;
    protected double transitivityTimerThreshold;
    protected Map<DTNHost, List<Duration>> connHistory;
    protected Map<DTNHost, Double> startTimestamps;

    /* Holds the contacts between this host and other hosts */
    protected Map<DTNHost, Double> localEncounters;

    public SnFDecisionEngineModRouter(Settings s) {
        if (s == null) {
            throw new IllegalArgumentException("Settings object cannot be null");
        }

        this.initialNrofCopies = s.getInt(NROF_COPIES);
        this.isBinary = s.getBoolean(BINARY_MODE);

        localEncounters = new HashMap<>();
    }

    public SnFDecisionEngineModRouter(SnFDecisionEngineModRouter r) {
        this.initialNrofCopies = r.initialNrofCopies;
        this.isBinary = r.isBinary;
        this.transitivityTimerThreshold = r.transitivityTimerThreshold;

        localEncounters = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        // do nothing
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double time = this.startTimestamps.getOrDefault(peer, 0.0);
        double etime = SimClock.getTime();

        // Find or create the connection history list
        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<>();
            connHistory.put(peer, history);
        } else {
            history = connHistory.get(peer);
        }
    
        // Add the new connection duration to the history
        Duration duration = new Duration(time, etime);
        history.add(duration);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost self = con.getOtherNode(peer);
		SnFDecisionEngineModRouter peerRouter = getDecisionEngine(peer);

		// update encounters of each host
		this.localEncounters.put(peer, SimClock.getTime());
		peerRouter.localEncounters.put(self, SimClock.getTime());

		// update transitivity
		//  ∀j= B : τB(j) < τA(j) −tm(dAB), set τA(j)=τB(j)+tm(dAB)
		// untuk setiap host (j) pada B (peer), lakukan:
		for (Map.Entry<DTNHost, Double> entry : peerRouter.localEncounters.entrySet()) {
			DTNHost host = entry.getKey();
			double peerLastEncounterTime = entry.getValue();
			// make the default zero of non-existent contact big to let it be overridden.
			double selfLastEncounterTime = this.localEncounters.getOrDefault(host, Double.POSITIVE_INFINITY);

			double distanceAB = self.getLocation().distance(peer.getLocation());

			// set default value since path could be null (i got NullPointerException lol)
			double selfSpeed = self.getPath() != null ? self.getPath().getSpeed() : 0.0;
			double peerSpeed = peer.getPath() != null ? peer.getPath().getSpeed() : 0.0;
			double expectedTimeToMove = distanceAB / Math.max(selfSpeed, peerSpeed);

			if (peerLastEncounterTime < selfLastEncounterTime - expectedTimeToMove) {
				this.localEncounters.put(host, peerLastEncounterTime + expectedTimeToMove);
			}
		}
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROP, initialNrofCopies);
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return aHost.equals(m.getTo());
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        if (!m.getTo().equals(thisHost)) {

			int nrofCopies = (int) m.getProperty(MSG_COUNT_PROP);
			if (isBinary) {
				nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
				m.updateProperty(MSG_COUNT_PROP, nrofCopies);
			} else {
				m.updateProperty(MSG_COUNT_PROP, --nrofCopies);
			}
			return true;
		}

		return false;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        // why would we need to forward the message if thisHost is the destination?
		if (m.getTo().equals(thisHost)) {
			return false;
		}

		// the other host is the message's destination! yay
		if (m.getTo().equals(otherHost)) {
			return true;
		}

		// not within the focus phase, give remaining copies
		if (((int) m.getProperty(MSG_COUNT_PROP) > 1) && (otherHost != null)) {
			return true;
		}

		/* FOCUS PHASE */

		DTNHost destination = m.getTo();
		//
		assert otherHost != null : "Other host should not be null!";
		SnFDecisionEngineModRouter peerRouter = getDecisionEngine(otherHost);

		return getAvgInterconnection(peerRouter.connHistory.get(destination)) > getAvgInterconnection(this.connHistory.get(destination));
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
		int nrofCopies = (int) m.getProperty(MSG_COUNT_PROP);

		if (nrofCopies <= 1) {
			return true;
		}

		if (isBinary) {
			nrofCopies = (int) Math.floor(nrofCopies / 2.0);
		} else {
			nrofCopies--;
		}

		m.updateProperty(MSG_COUNT_PROP, nrofCopies);

		return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }

    @Override
    public void update(DTNHost thisHost) {
        // do nothing
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SnFDecisionEngineModRouter(this);
    }

    private double getAvgInterconnection(List<Duration> history) {
        if (history.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        Iterator<Duration> it = history.iterator();
        Duration after = it.next();
        Duration before;

        while (it.hasNext()) {
            before = after;
            after = it.next();
            sum += after.start - before.end;
        }

        return sum / history.size();
    }

    private SnFDecisionEngineModRouter getDecisionEngine(DTNHost host) {
        DecisionEngineRouter router = ((DecisionEngineRouter) host.getRouter());
        assert router instanceof DecisionEngineRouter : "This router only works " + " with other routers of same type";
        return (SnFDecisionEngineModRouter) router.getDecisionEngine();
    }
}
