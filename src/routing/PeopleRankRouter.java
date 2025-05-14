package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import routing.community.Duration;

public class PeopleRankRouter implements RoutingDecisionEngine {

    public final static String PEOPLE_RANK_NS = "PeopleRankRouter";
    public final static String DAMPING_FACTOR_STRING = "dampingFactor";
    public final static String MIN_DURATION = "minDuration";

    protected Map<DTNHost, Tuple<Double, Integer>> peopleRankScores;
    protected Map<DTNHost, List<Duration>> connHistory;
    protected Map<DTNHost, Double> startTimestamps;

    protected double dampingFactor;
    protected double minDuration;

    /**
     * Constructor for PeopleRank based on the specified settings.
     *
     * @param s The settings object containing configuration parameters
     */
    public PeopleRankRouter(Settings s) {
        this.dampingFactor = s.getDouble(DAMPING_FACTOR_STRING);
        this.minDuration = s.getDouble(MIN_DURATION);

        this.peopleRankScores = new HashMap<>();
        this.connHistory = new HashMap<>();
        this.startTimestamps = new HashMap<>();
    }

    /**
     * Copy constructor for PeopleRank.
     *
     * @param r The PeopleRank object to replicate
     */
    public PeopleRankRouter(PeopleRankRouter r) {
        this.dampingFactor = r.dampingFactor;
        this.minDuration = r.minDuration;

        this.peopleRankScores = new HashMap<>();
        this.connHistory = new HashMap<>();
        this.startTimestamps = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {}

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double time = this.startTimestamps.getOrDefault(peer, 0.0);
        double etime = SimClock.getTime();

        // Find or create the connection history list
        List<Duration> history;
        if (!this.connHistory.containsKey(peer)) {
            history = new LinkedList<>();
            this.connHistory.put(peer, history);
        } else {
            history = this.connHistory.get(peer);
        }
        history.add(new Duration(time, etime));

        // add this connection to the list
        if (etime - time > minDuration) {
            updateRank(peer);
            getDecisionEngine(peer).updateRank(thisHost);
        }

		this.startTimestamps.remove(peer);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        PeopleRankRouter de = this.getDecisionEngine(peer);

        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());
    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo().equals(aHost);
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return true;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo().equals(otherHost)) {
            return true;
        }
		
        return (getPeopleRankScore(thisHost) < getPeopleRankScore(otherHost));
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
        // Do Nothing
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new PeopleRankRouter(this);
    }

    // Helper Methods
    public void updateRank(DTNHost host) {
        int neighborCount = getNeighborCount(host);
        peopleRankScores.put(host, new Tuple<>(getPeopleRankScore(host),neighborCount + 1 ));
    }

    private PeopleRankRouter getDecisionEngine(DTNHost host) {
        DecisionEngineRouter router = ((DecisionEngineRouter) host.getRouter());
        assert router instanceof DecisionEngineRouter : "This router only works with other routers of same type";
        return (PeopleRankRouter) router.getDecisionEngine();
    }

    private double getPeopleRankScore(DTNHost host) {
        PeopleRankRouter hostRouter = getDecisionEngine(host);

        double sum = 0;

        for (Tuple<Double, Integer> tup : hostRouter.peopleRankScores.values()) {
            // host's peoplerank score / host's nrof neighbors
            sum += tup.getKey() / tup.getValue();
        }

        return (1 - this.dampingFactor) + (this.dampingFactor * sum);
    }

    private int getNeighborCount(DTNHost host) {
        return getDecisionEngine(host).peopleRankScores.get(host).getValue();
    }
}
