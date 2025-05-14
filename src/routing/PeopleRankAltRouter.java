package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import routing.community.Duration;

public class PeopleRankAltRouter implements RoutingDecisionEngine {

    public final static String PEOPLE_RANK_NS = "PeopleRankAltRouter";
    public final static String DAMPING_FACTOR_STRING = "dampingFactor";
    public final static String MAX_INTERCONNECTION_STRING = "maxInterconnection";

    protected Map<DTNHost, Tuple<Double, Integer>> peopleRankScores;
    protected Map<DTNHost, List<Duration>> connHistory;
    protected Map<DTNHost, Double> startTimestamps;
    protected Set<DTNHost> neigbors;

    protected double dampingFactor;
    protected double maxInterconnection;

    /**
     * Constructor for PeopleRank based on the specified settings.
     *
     * @param s The settings object containing configuration parameters
     */
    public PeopleRankAltRouter(Settings s) {
        this.dampingFactor = s.getDouble(DAMPING_FACTOR_STRING);
        this.maxInterconnection = s.getDouble(MAX_INTERCONNECTION_STRING);

        this.peopleRankScores = new HashMap<>();
        this.connHistory = new HashMap<>();
        this.startTimestamps = new HashMap<>();
        this.neigbors = new HashSet<>();
    }

    /**
     * Copy constructor for PeopleRank.
     *
     * @param r The PeopleRank object to replicate
     */
    public PeopleRankAltRouter(PeopleRankAltRouter r) {
        this.dampingFactor = r.dampingFactor;
        this.maxInterconnection = r.maxInterconnection;

        this.peopleRankScores = new HashMap<>();
        this.connHistory = new HashMap<>();
        this.startTimestamps = new HashMap<>();
        this.neigbors = new HashSet<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        for (Map.Entry<DTNHost, List<Duration>> entry : this.connHistory.entrySet()) {
            DTNHost host = entry.getKey();

            double hostRank = getPeopleRankScore(host);

            this.peopleRankScores.put(host, new Tuple<>(hostRank, 1));
        }
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

        // add this connection to the list
        if (this.maxInterconnection > getAvgInterconnection(history)) {
            history.add(new Duration(time, etime));
            this.neigbors.add(peer);
        }

        this.startTimestamps.remove(peer);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        PeopleRankAltRouter de = this.getDecisionEngine(peer);

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
        return new PeopleRankAltRouter(this);
    }

    // Helper Methods
    public Map<DTNHost, Tuple<Double, Integer>> getPeopleRankScores() {
        return peopleRankScores;
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

    private PeopleRankAltRouter getDecisionEngine(DTNHost host) {
        DecisionEngineRouter router = ((DecisionEngineRouter) host.getRouter());
        assert router instanceof DecisionEngineRouter : "This router only works " + " with other routers of same type";
        return (PeopleRankAltRouter) router.getDecisionEngine();
    }

    private double getPeopleRankScore(DTNHost host) {
        PeopleRankAltRouter hostRouter = getDecisionEngine(host);

        double sum = 0;

        for (Tuple<Double, Integer> tup : hostRouter.getPeopleRankScores().values()) {
            // host's peoplerank score / host's nrof neighbors
            sum += tup.getKey() / tup.getValue();
        }

        return (1 - this.dampingFactor) + (this.dampingFactor * sum);
    }
}
