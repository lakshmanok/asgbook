package edu.ou.asgbook.filters;

import edu.ou.asgbook.core.LatLonGrid;

public interface SpatialFilter {
	public LatLonGrid filter(final LatLonGrid input);
}
