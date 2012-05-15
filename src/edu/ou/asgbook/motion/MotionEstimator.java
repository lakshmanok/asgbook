package edu.ou.asgbook.motion;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pair;

public interface MotionEstimator {

	/**
	 * returns motion in the two directions. The first one is north to south
	 * and the second one is east to west.
	 * The data is aligned to second time frame.
	 * The output dir is used for intermediate products and may be null.
	 */
	public Pair<LatLonGrid, LatLonGrid> compute(LatLonGrid data0, LatLonGrid data1, File outdir);

}