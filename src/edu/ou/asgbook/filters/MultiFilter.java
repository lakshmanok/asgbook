/**
 * 
 */
package edu.ou.asgbook.filters;

import edu.ou.asgbook.core.LatLonGrid;

/**
 * Carries out multiple operations.
 * @author Valliappa.Lakshmanan
 *
 */
public class MultiFilter implements SpatialFilter {
	private SpatialFilter[] filters;
	private int numTimes;
		
	public MultiFilter(SpatialFilter[] filters, int numTimes) {
		super();
		this.filters = filters;
		this.numTimes = numTimes;
	}

	@Override
	public LatLonGrid filter(LatLonGrid input) {
		LatLonGrid output = LatLonGrid.copyOf(input);
		for (int i=0; i < numTimes; ++i){
			for (SpatialFilter filter : filters){
				output = filter.filter(output);
			}
		}
		return output;
	}
}
