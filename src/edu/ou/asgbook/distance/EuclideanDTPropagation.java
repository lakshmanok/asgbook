/**
 * 
 */
package edu.ou.asgbook.distance;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Implementation of Euclidean distance that updates the distance
 * instead of computing it afresh each time.
 * @author v.lakshmanan
 * 
 */
public class EuclideanDTPropagation implements EuclideanDT {

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
					propagate(dist, i, j, 250*250);
				}
			}
		return dist;
	}

	private void propagate(LatLonGrid dist, int cx, int cy, int maxdist) {
		List<Pixel> pixels = new ArrayList<Pixel>();
		pixels.add(new Pixel(cx,cy,0));
		int n = 0;
		while (pixels.size() > 0) {
			Pixel p = pixels.get(pixels.size() - 1);
			int x = p.getX();
			int y = p.getY();
			pixels.remove(pixels.size() - 1); // pop
			for (int di = - 1; di <= 1; ++di){
				int i = x + di;
				for (int dj = - 1; dj <= 1; ++dj) {
					int j = y + dj;
					if (dist.isValid(i, j)) {
						int newdist = (i - cx) * (i - cx) + (j - cy) * (j - cy);
						if (newdist < dist.getValue(i, j) && newdist < maxdist) {
							dist.setValue(i, j, newdist);
							pixels.add(new Pixel(i, j, 0)); // propagate from here
							++n;
						}
					}
				}
			}
		}
		// System.out.println(n + " pixels' distance set starting at " + cx + " " + cy);
	}

	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("distance");
		LatLonGrid popdensity = GlobalPopulation
				.read(GlobalPopulation.NORTHAMERICA);

		EuclideanDT transform = new EuclideanDTPropagation();
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
		KmlWriter.write(edt, out, "edtupdate", PngWriter.createCoolToWarmColormap());
	}
}
