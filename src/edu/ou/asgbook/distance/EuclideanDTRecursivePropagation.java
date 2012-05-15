/**
 * 
 */
package edu.ou.asgbook.distance;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Note that this class is only for illustrative purposes. It will not work
 * because of stack overflow. Use the EuclideanDTPropagation implementation
 * that replaces the recursion by a list.
 * @author v.lakshmanan
 * 
 */
public class EuclideanDTRecursivePropagation implements EuclideanDT {

	@Override
	public LatLonGrid getDistanceTransform(LatLonGrid data, int thresh) {
		int nrows = data.getNumLat();
		int ncols = data.getNumLon();
		final int MAXDIST = nrows * nrows + ncols * ncols;
		LatLonGrid dist = new LatLonGrid(nrows, ncols, MAXDIST,
				data.getNwCorner(), data.getLatRes(), data.getLonRes());
		dist.fill(dist.getMissing());
		for (int i = 0; i < nrows; ++i)
			for (int j = 0; j < ncols; ++j) {
				if (data.getValue(i, j) > thresh) {
					dist.setValue(i, j, 0);
					propagate(dist, i, j, i, j);
				}
			}
		return dist;
	}

	/** Propagate from ax,ay. The seed pixel is at (cx,cy) */
	private void propagate(LatLonGrid dist, int cx, int cy, int ax, int ay) {
		for (int i = ax - 1; i <= ax + 1; ++i)
			for (int j = ay - 1; j <= ay + 1; ++j) {
				if (dist.isValid(i, j)) {
					int newdist = (i - cx) * (i - cx) + (j - cy) * (j - cy);
					if (newdist < dist.getValue(i, j)) {
						dist.setValue(i, j, newdist);
						propagate(dist,cx,cy,i,j);
					}
				}
			}
	}

	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("distance");
		LatLonGrid popdensity = GlobalPopulation
				.read(GlobalPopulation.NORTHAMERICA);

		EuclideanDT transform = new EuclideanDTRecursivePropagation();
		LatLonGrid edt = transform.getDistanceTransform(popdensity, 50);
		
		// write it clamped out at a reasonable distance
		final int maxdist = 250 * 250;
		for (int i=0; i < edt.getNumLat(); ++i){
			for (int j=0; j < edt.getNumLon(); ++j){
				if ( edt.getValue(i,j) > maxdist){
					edt.setValue(i,j, edt.getMissing() );
				}
			}
		}
		KmlWriter.write(edt, out, "edt", PngWriter.createCoolToWarmColormap());
	}
}
