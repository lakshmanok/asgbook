/**
 * 
 */
package edu.ou.asgbook.segmentation;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Objects consist of pixels that are > thresh2 but have at least one pixel > thresh1
 * @author v.lakshmanan
 *
 */
public class HysteresisSegmenter implements Segmenter {
	private int t1, t2;
	
	public HysteresisSegmenter(int thresh1, int thresh2) {
		super();
		this.t1 = thresh1;
		this.t2 = thresh2;
		if (t1 < t2){
			// swap
			int t = t1;
			t1 = t2;
			t2 = t;
		}
	}

	@Override
	public LabelResult label(LatLonGrid data){
		final int UNSET = 0;
		int nrows = data.getNumLat();
		int ncols = data.getNumLon();
		LatLonGrid label = new LatLonGrid(nrows,ncols,0,data.getNwCorner(),data.getLatRes(),data.getLonRes());
		// label.fill(UNSET); java default is to zero-out arrays
		int regno = 0;
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			if ( data.getValue(i, j) > t1 && label.getValue(i, j) == UNSET ){
				++regno;
				RegionGrowing.growRegion(i,j, data, t2, label, regno);
			}
		}
		System.out.println("Found " + (regno+1) + " objects");
		return new LabelResult(label, regno);
	}
	
	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("hysteresis");
		
		// data
		LatLonGrid grid = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LinearScaling()).crop(1000, 2100, 100, 200);
		KmlWriter.write(grid, out, "orig", PngWriter.createCoolToWarmColormap());
		
		// hysteresis thresh
		for (int thresh = 10; thresh <= 30; thresh += 10){
			int t1 = thresh;
			int t2 = thresh-5;
			Segmenter seg = new HysteresisSegmenter(t1, t2);
			LatLonGrid label = seg.label(grid).label;
			// label.setMissing(-1); // so background is present
			KmlWriter.write(label, out, "cities_"+t1+"_"+t2, PngWriter.createRandomColormap());
		}
	}
}
