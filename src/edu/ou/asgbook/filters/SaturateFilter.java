/**
 * 
 */
package edu.ou.asgbook.filters;

import edu.ou.asgbook.core.LatLonGrid;

/**
 * Sets all values < MIN to MIN and all values > MAX to MAX
 * @author Valliappa.Lakshmanan
 *
 */
public class SaturateFilter implements SpatialFilter {
	private final int min, max;
	
	public SaturateFilter(int min, int max) {
		this.min = min;
		this.max = max;
	}
	
	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return saturate(input);
	}

	public LatLonGrid saturate(final LatLonGrid input){
		LatLonGrid output = LatLonGrid.copyOf(input);
		int[][] outData = output.getData();
		int[][] inData = input.getData();
		for (int i=0; i < output.getNumLat(); ++i){
			for (int j=0; j < output.getNumLon(); ++j){
				int inval = inData[i][j];
				if ( inval < min || inval == input.getMissing() ){
					outData[i][j] = min;
				} else if ( inval > max ){
					outData[i][j] = max;
				}
			}
		}
		return output;
	}

}
