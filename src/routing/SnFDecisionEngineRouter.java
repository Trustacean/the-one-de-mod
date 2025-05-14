/* 
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import java.util.Map;

public class SnFDecisionEngineRouter implements RoutingDecisionEngine {
    /**
     * identifier for the initial number of copies setting ({@value})
     */
    public static final String NROF_COPIES_S = "nrofCopies";
    /**
     * identifier for the difference in timer values needed to forward on a
     * message copy
     */
    public static final String TIMER_THRESHOLD_S = "transitivityTimerThreshold";
    /**
     * Message property key for the remaining available copies of a message
     */
    public static final String MSG_COUNT_PROP = "SprayAndFocus.copies";

	protected int initialNrofCopies;
	protected double transitivityTimerThreshold;
    protected Map<String, Double> forwardingDecisionTable;
	
    public SnFDecisionEngineRouter(Settings s) {
        this.initialNrofCopies = s.getInt(NROF_COPIES_S);
        this.transitivityTimerThreshold = s.getDouble(TIMER_THRESHOLD_S);
    }

    /**
     * Copy constructor.
     *
     * @param r The router prototype where setting values are copied from
     */
    protected SnFDecisionEngineRouter(SnFDecisionEngineRouter r) {
        this.initialNrofCopies = r.initialNrofCopies;
        this.transitivityTimerThreshold = r.transitivityTimerThreshold;
        this.forwardingDecisionTable = r.forwardingDecisionTable;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        // Do nothing
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        // Do nothing
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        // Do nothing
    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return true;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        return true;
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
    public SnFDecisionEngineRouter replicate() {
        return new SnFDecisionEngineRouter(this);
    }

    @Override
    public void update(DTNHost thisHost) {
        // Do nothing
    }
}
