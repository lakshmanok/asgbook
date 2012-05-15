/**
 * 
 */
package edu.ou.asgbook.filters;

import java.io.File;
import java.util.Random;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Denoising filter that removes speckle.
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class SpeckleFilter implements SpatialFilter {
	private final MedianFilter smFilter;
	private final int maxChange;
	
	public SpeckleFilter(int halfSize, int maxChange) {
		this.smFilter = new MedianFilter(halfSize);
		this.maxChange = maxChange;
	}
	
	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return speckleFilter(input);
	}

	public LatLonGrid speckleFilter(final LatLonGrid input){
		LatLonGrid smoothed = smFilter.filter(input);
		LatLonGrid output = LatLonGrid.copyOf(input);
		int[][] inData = input.getData();
		int[][] smData = smoothed.getData();
		int nx = inData.length;
		int ny = inData[0].length;
		for (int i=0; i < nx; ++i){
			for (int j=0; j < ny; ++j){
				if (inData[i][j] != input.getMissing() &&
					smData[i][j] != smoothed.getMissing()){
					int diff = Math.abs(inData[i][j] - smData[i][j]);
					if (diff > maxChange){ // noise
						output.setValue(i,j, smData[i][j]);
					}
				}
			}
		}
		return output;
	}

	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("speckle");
		
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());

		// add noise
		Random rand = new Random();
		final int NOISE = 200;
		for (int i=0; i < 1000; ++i){
			int x = rand.nextInt(popdensity.getNumLat());
			int y = rand.nextInt(popdensity.getNumLon());
			int add = NOISE + rand.nextInt(NOISE/2);
			popdensity.setValue(x, y, popdensity.getValue(x,y) + add);
		}
		KmlWriter.write(popdensity, out, "noisy", PngWriter.createCoolToWarmColormap());
		
		// dilate
		LatLonGrid dilate1 = new SpeckleFilter(3,NOISE).filter(popdensity);
		KmlWriter.write(dilate1, out, "speckle_3", PngWriter.createCoolToWarmColormap());
	}
}
