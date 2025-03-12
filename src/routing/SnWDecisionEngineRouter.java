/* 
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;

public class SnWDecisionEngineRouter implements RoutingDecisionEngine {

    public static final String NROF_COPIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouter";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "." + "copies";

    protected int initialNrofCopies;
    protected boolean isBinary;

    public SnWDecisionEngineRouter(Settings s) {
        if (s == null) {
            s = new Settings(SPRAYANDWAIT_NS);
        }

        this.initialNrofCopies = s.getInt(NROF_COPIES);
        this.isBinary = s.getBoolean(BINARY_MODE);
    }

    /**
     * Copy constructor.
     *
     * @param r The router prototype where setting values are copied from
     */
    protected SnWDecisionEngineRouter(SnWDecisionEngineRouter r) {
        this.initialNrofCopies = r.initialNrofCopies;
        this.isBinary = r.isBinary;
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
        m.addProperty(MSG_COUNT_PROPERTY, this.initialNrofCopies);
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        int nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);

        if (isBinary) {
            nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
        } else {
            nrofCopies = 1;
        }

        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        return !m.getTo().equals(thisHost);
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost) {
            return true;
        }
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);

        return nrofCopies > 1;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        if (m.getTo() == otherHost) {
            return false;
        }
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        if (isBinary) {
            nrofCopies = (int) Math.floor(nrofCopies / 2.0);
        } else {
            nrofCopies--;
        }

        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }

    @Override
    public SnWDecisionEngineRouter replicate() {
        return new SnWDecisionEngineRouter(this);
    }

    @Override
    public void update(DTNHost thisHost) {
        // Do nothing
    }
}
