/**
 * 
 */
package edu.ou.asgbook.segmentation;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.filters.SimpleThresholder;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Simple object identification based on a single threshold.
 * 
 * @author v.lakshmanan
 *
 */
public class ThresholdSegmenter implements Segmenter {
	private int thresh;
	
	public ThresholdSegmenter(int thresh) {
		super();
		this.thresh = thresh;
	}

	/**
	 * Creates a labeled grid where background pixels are set to 0
	 * and labels for objects go 1,2,3... All pixels > thresh are
	 * part of an object.
	 */
	public LabelResult label(LatLonGrid data){
		final int UNSET = 0;
		int nrows = data.getNumLat();
		int ncols = data.getNumLon();
		LatLonGrid label = new LatLonGrid(nrows,ncols,0,data.getNwCorner(),data.getLatRes(),data.getLonRes());
		// label.fill(UNSET); java default is to zero-out arrays
		int regno = 0;
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			if ( data.getValue(i, j) > thresh && label.getValue(i, j) == UNSET ){
				++regno;
				RegionGrowing.growRegion(i,j, data, thresh, label, regno);
			}
		}
		System.out.println("Found " + (regno+1) + " objects");
		return new LabelResult(label, regno);
	}
	
	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("regiongrowing");
		
		// data
		LatLonGrid grid = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LinearScaling()).crop(1000, 2100, 100, 200);
		KmlWriter.write(grid, out, "orig", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(1000, 2100, 100, 200), out, "orig2", PngWriter.createCoolToWarmColormap());
		
		// global thresh
		for (int thresh = 10; thresh <= 30; thresh += 10){
			KmlWriter.write(new SimpleThresholder(thresh).threshold(grid), out, "thresh_"+thresh, PngWriter.createCoolToWarmColormap());
			ThresholdSegmenter seg = new ThresholdSegmenter(thresh);
			LatLonGrid label = seg.label(grid).label;
			// label.setMissing(-1); // so background is present
			KmlWriter.write(label, out, "cities_"+thresh, PngWriter.createRandomColormap());
		}
	}
}
