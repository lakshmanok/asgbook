/**
 * 
 */
package edu.ou.asgbook.filters;

import edu.ou.asgbook.core.LatLonGrid;

/**
 * at every pixel, replaces its value (val) by (A - val)
 * @author Valliappa.Lakshmanan
 *
 */
public class Inverter implements SpatialFilter {
	private int A;
	
	public Inverter(int A) {
		this.A = A;
	}
	
	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return invert(input);
	}

	public LatLonGrid invert(final LatLonGrid input){
		LatLonGrid output = LatLonGrid.copyOf(input);
		int[][] outData = output.getData();
		int[][] inData = input.getData();
		for (int i=0; i < output.getNumLat(); ++i){
			for (int j=0; j < output.getNumLon(); ++j){
				if ( inData[i][j] != input.getMissing() ){
					outData[i][j] = A - inData[i][j];
				}
			}
		}
		return output;
	}
}
