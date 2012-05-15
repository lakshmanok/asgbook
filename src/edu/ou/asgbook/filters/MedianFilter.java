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
 * A smoothing operation that involves replacing a pixel by the local median.
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class MedianFilter implements SpatialFilter {
	private int halfSize;
	
	public MedianFilter(int halfSize) {
		this.halfSize = halfSize;
	}
	
	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return smooth(input);
	}

	public LatLonGrid smooth(final LatLonGrid input){
		LatLonGrid output = LatLonGrid.copyOf(input);
		output.fill(output.getMissing());
		int[][] outData = output.getData();
		int[][] inData = input.getData();
		int hx = halfSize;
		int hy = halfSize;
		int nx = inData.length;
		int ny = inData[0].length;
		int[] arr = new int[(2*hx+1)*(2*hy+1)];
		for (int i=hx; i < (nx-hx); ++i){
			for (int j=hy; j < (ny-hy); ++j){
				int nelements = 0;
				for (int m=-hx; m <= hx; ++m){
					for (int n=-hy; n <= hy; ++n){
						int inval = inData[i+m][j+n];
						if (inval != input.getMissing()){
							arr[nelements] = inval;
							++nelements;
						}
					}
				}
				if (nelements > 0){
					outData[i][j] = QuickSelect.kth_element(arr, nelements, nelements/2);
				}
			}
		}
		return output;
	}

	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("median");
		
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());
		
		// dilate
		LatLonGrid dilate1 = new MedianFilter(1).smooth(popdensity);
		KmlWriter.write(dilate1, out, "median_3", PngWriter.createCoolToWarmColormap());
		LatLonGrid dilate3 = new MedianFilter(3).smooth(popdensity);
		KmlWriter.write(dilate3, out, "median_7", PngWriter.createCoolToWarmColormap());
		LatLonGrid dilate5 = new MedianFilter(5).smooth(popdensity);
		KmlWriter.write(dilate5, out, "median_11", PngWriter.createCoolToWarmColormap());
	}
}
