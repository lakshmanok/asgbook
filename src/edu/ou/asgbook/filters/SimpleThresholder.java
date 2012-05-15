/**
 * 
 */
package edu.ou.asgbook.filters;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Replace pixel values with 1 or 0 depending on whether they
 * are above or below a single threshold.
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class SimpleThresholder implements SpatialFilter {
	private int thresh;
	
	public SimpleThresholder(int thresh) {
		this.thresh = thresh;
	}
	
	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return threshold(input);
	}

	public LatLonGrid threshold(final LatLonGrid input){
		LatLonGrid output = LatLonGrid.copyOf(input);
		int[][] outData = output.getData();
		int[][] inData = input.getData();
		for (int i=0; i < output.getNumLat(); ++i){
			for (int j=0; j < output.getNumLon(); ++j){
				outData[i][j] = (inData[i][j] >= thresh)? 1 : 0;
			}
		}
		return output;
	}

	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("threshold");
		
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA);
		popdensity.setMissing(0); // transparent
		KmlWriter.write(popdensity, out, "popdensity", PngWriter.createCoolToWarmColormap());
		
		// threshold
		SimpleThresholder filter = new SimpleThresholder(100);
		LatLonGrid thresh = filter.threshold(popdensity);
		KmlWriter.write(thresh, out, "highdensity", PngWriter.createCoolToWarmColormap());
	}
}
