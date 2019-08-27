package org.opentripplanner.routing.routepreferences;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default route preference rules for HSL.
 * Currently only used for adding default unpreferred routes.
 * 
 * @author optionsome
 * @see RoutePreferencesSource
 */
public class BlumenauSCRoutePreferencesSource implements RoutePreferencesSource {
    
    @Override
    public void setRoutePreferences(RoutingRequest routingRequest, Graph graph) { }
}
