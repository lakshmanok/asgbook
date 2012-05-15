package edu.ou.asgbook.segmentation;

import edu.ou.asgbook.core.LatLonGrid;

/**
 * Result of segmentation.  Each pixel holds the region number that it belongs to.
 * Zero is the background value.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class LabelResult {
	public final LatLonGrid label;
	public final int maxlabel;
	public LabelResult(LatLonGrid label, int maxlabel) {
		this.label = label;
		this.maxlabel = maxlabel;
	}
}
