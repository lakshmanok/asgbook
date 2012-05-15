/**
 * 
 */
package edu.ou.asgbook.motion;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pair;

/**
 * Aligns two grids and then computes their difference.
 * @author v.lakshmanan
 *
 */
public class AlignAndDifference {

	public LatLonGrid compute(LatLonGrid data0, LatLonGrid data1, Pair<LatLonGrid, LatLonGrid> uv) {
		return compute(data0, data1, uv, 1);
	}
	
	public LatLonGrid compute(LatLonGrid data0, LatLonGrid data1, Pair<LatLonGrid, LatLonGrid> uv, int MOT_SCALE) {
		LatLonGrid result = LatLonGrid.copyOf(data1);

		final float mot_scale = MOT_SCALE; // integer division truncates
		for (int i=0; i < result.getNumLat(); ++i){
			for (int j=0; j < result.getNumLon(); ++j){
				// align by moving data0 to match up with data1
				// then compute difference
				int aligned0 = data0.getValue(i,j);
				// find motion at this point
				int motx = Math.round(uv.first.getValue(i,j) / mot_scale);
				int moty = Math.round(uv.second.getValue(i,j) / mot_scale);
				// grab pixel from old location
				int oldx = i - motx;
				int oldy = j - moty;
				if (data0.isValid(oldx, oldy)){
					aligned0 = data0.getValue(oldx, oldy);
				}
				int diff = data1.getValue(i,j) - aligned0;
				result.setValue(i,j, diff);
			}
		}

		return result;
	}
}
