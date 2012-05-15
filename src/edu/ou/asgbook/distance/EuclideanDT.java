package edu.ou.asgbook.distance;

import edu.ou.asgbook.core.LatLonGrid;

public interface EuclideanDT {

	/**
	 * At every pixel, finds the square of the Euclidean distance to the nearest
	 * pixel > thresh
	 */
	public abstract LatLonGrid getDistanceTransform(LatLonGrid data, int thresh);
}