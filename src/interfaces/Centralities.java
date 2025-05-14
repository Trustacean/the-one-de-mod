package interfaces;

import core.DTNHost;
import java.util.List;
import java.util.Map;
import routing.community.Duration;

public interface Centralities {
    public double[] getGlobalCentralities(Map<DTNHost, List<Duration>> connHistory);
}