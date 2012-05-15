package edu.ou.asgbook.segmentation;

import edu.ou.asgbook.core.LatLonGrid;

/**
 * Object identification technique.
 * 
 * @author valliappa.lakshmanan
 *
 */
public interface Segmenter {

	/**
	 * Creates a labeled grid where background pixels are set to 0
	 * and labels for objects go 1,2,3... All pixels > thresh are
	 * part of an object.
	 */
	public abstract LabelResult label(LatLonGrid data);

}