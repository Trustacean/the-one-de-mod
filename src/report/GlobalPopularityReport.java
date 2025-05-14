/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import core.DTNHost;
import core.SimScenario;
import java.util.List;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.community.BubbleRap;

/**
 * Reports delivered messages
 * report: 
 *  message_id creation_time deliver_time (duplicate)
 */
public class GlobalPopularityReport extends Report {
	// private int interval;
	// private double lastRecord;
	// private Map<DTNHost, List<Double>> popularityMap;
	/** all message delays */
	
	/**
	 * Constructor.
	 */
	public GlobalPopularityReport() {
		init();
	}
	
	@Override
	public void init() {
		super.init();
		// this.lastRecord = 0;
		// this.interval = 86400; // 1 day
		// this.popularityMap = new HashMap<>();
	}

	// @Override
	// public void updated(List<DTNHost> hosts) {
	// 	if (SimClock.getTime() - lastRecord >= interval) {
	// 		for (DTNHost host : hosts) {
	// 			MessageRouter router = (MessageRouter) host.getRouter();
	// 			DistributedBubbleRap de = (DistributedBubbleRap) ((DecisionEngineRouter) router).getDecisionEngine();
	// 			double popularity = de.getCentrality();
	// 			popularityMap.putIfAbsent(host, new ArrayList<>());
    //             popularityMap.get(host).add(popularity);
	// 		}
	// 		lastRecord = SimClock.getTime();
    //     }
	// }

    // @Override
    // public void done() {
    //     int maxIntervals = popularityMap.values().stream()
    //             .mapToInt(List::size)
    //             .max()
    //             .orElse(0);
        
    //     for (Map.Entry<DTNHost, List<Double>> entry : popularityMap.entrySet()) {
    //         StringBuilder line = new StringBuilder();
    //         line.append(entry.getKey());
            
    //         for (Double popularity : entry.getValue()) {
    //             line.append(" ").append(popularity.intValue());
    //         }
            
    //         write(line.toString());
    //     }
    //     super.done();
    // }

	@Override
	public void done() {
		List<DTNHost> hosts = SimScenario.getInstance().getHosts();
		write("NodeID");
		for (DTNHost host : hosts) {
			StringBuilder line = new StringBuilder();
			line.append(host);
			MessageRouter router = (MessageRouter) host.getRouter();
			BubbleRap de = (BubbleRap) ((DecisionEngineRouter) router).getDecisionEngine();
			double[] centralities = de.getGlobalCentralities();
			for (int i = 0; i < centralities.length; i++) {
				line.append(" ").append(centralities[i]);
			}
			write(line.toString());
		}
		super.done();
	}
}
