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
 * The Saito technique of computing the distance transform by calculating in
 * the two directions separately.
 * 
 * @author v.lakshmanan
 * 
 */
public class EuclideanDTSaito implements EuclideanDT {

	@Override
	public LatLonGrid getDistanceTransform(LatLonGrid data, int thresh) {
		try {
			return getDistanceTransform(data, thresh, null);
		} catch (Exception e) {
			throw new IllegalStateException();
		}
	}

	private LatLonGrid getDistanceTransform(LatLonGrid data, int thresh,
			File outputDir) throws Exception {
		LatLonGrid Gij = computeFirstStage(data, thresh);

		if (outputDir != null) {
			writeClamped(Gij, outputDir, "stage1");
		}

		LatLonGrid Sij = computeSecondStage(Gij);

		if (outputDir != null) {		
			writeClamped(Sij, outputDir, "stage2");
		}

		return Sij;
	}

	private LatLonGrid computeSecondStage(LatLonGrid gij) {
		int nrows = gij.getNumLat();
		int ncols = gij.getNumLon();
		final int MAXDIST = nrows * nrows + ncols * ncols;
		LatLonGrid sij = new LatLonGrid(nrows, ncols, MAXDIST,
				gij.getNwCorner(), gij.getLatRes(), gij.getLonRes());
		for (int i = 0; i < nrows; ++i) {
			for (int j = 0; j < ncols; ++j) {
				int mindist = MAXDIST;
				for (int x = 0; x < nrows; ++x) {
					int dist = gij.getValue(x, j) + (i - x) * (i - x);
					mindist = Math.min(dist, mindist);
				}
				sij.setValue(i, j, mindist);
			}
		}
		return sij;
	}

	/**
	 * first stage of an independent scanning algorithm
	 */
	private LatLonGrid computeFirstStage(LatLonGrid data, int thresh) {
		int nrows = data.getNumLat();
		int ncols = data.getNumLon();
		final int MAXDIST = nrows * nrows + ncols * ncols;
		LatLonGrid dist1d = new LatLonGrid(nrows, ncols, MAXDIST,
				data.getNwCorner(), data.getLatRes(), data.getLonRes());
		for (int i = 0; i < nrows; ++i) {
			// at each row, find the distance to nearest point
			// we can do this by marching forwards, then backwards
			int prevj_above_thresh = -1;
			for (int j = 0; j < ncols; ++j) {
				if (data.getValue(i, j) > thresh) {
					prevj_above_thresh = j;
					dist1d.setValue(i, j, 0);
				} else if (prevj_above_thresh < 0) {
					dist1d.setValue(i, j, MAXDIST);
				} else {
					int dist = (j - prevj_above_thresh);
					dist1d.setValue(i, j, dist * dist);
				}
			}
			// then march backwards
			prevj_above_thresh = -1;
			for (int j = ncols - 1; j >= 0; --j) {
				if (data.getValue(i, j) > thresh) {
					prevj_above_thresh = j;
				} else if (prevj_above_thresh < 0) {
					// whatever value is there now will be <= MAXDIST
				} else {
					int dist = (j - prevj_above_thresh);
					int prevdist = dist1d.getValue(i, j);
					int mindist = Math.min(prevdist, dist * dist);
					dist1d.setValue(i, j, mindist);
				}
			}
		}
		return dist1d;
	}

	private static void writeClamped(LatLonGrid dist, File out, String name) throws Exception {
		// write it clamped out at a reasonable distance
		LatLonGrid edt = LatLonGrid.copyOf(dist);
		final int maxdist = 250 * 250;
		for (int i=0; i < edt.getNumLat(); ++i){
			for (int j=0; j < edt.getNumLon(); ++j){
				if ( edt.getValue(i,j) > maxdist){
					edt.setValue(i,j, edt.getMissing() );
				}
			}
		}
		KmlWriter.write(edt, out, name, PngWriter.createCoolToWarmColormap());
	}
	
	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("euclideandt");
		LatLonGrid popdensity = GlobalPopulation
				.read(GlobalPopulation.NORTHAMERICA);

		EuclideanDTSaito transform = new EuclideanDTSaito();
		LatLonGrid edt = transform.getDistanceTransform(popdensity, 50, out);
		writeClamped(edt, out, "edt");
	}
}
