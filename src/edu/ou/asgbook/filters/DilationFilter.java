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
 * Expands entities by taking a local maximum.
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class DilationFilter implements SpatialFilter {
	private int halfSize;
	
	public DilationFilter(int halfSize) {
		this.halfSize = halfSize;
	}
	
	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return dilate(input);
	}

	public LatLonGrid dilate(final LatLonGrid input){
		LatLonGrid output = LatLonGrid.copyOf(input);
		output.fill(output.getMissing());
		int[][] outData = output.getData();
		int[][] inData = input.getData();
		int hx = halfSize;
		int hy = halfSize;
		int nx = inData.length;
		int ny = inData[0].length;
		for (int i=hx; i < (nx-hx); ++i){
			for (int j=hy; j < (ny-hy); ++j){
				int max = input.getMissing();
				boolean set = false;
				for (int m=-hx; m <= hx; ++m){
					for (int n=-hy; n <= hy; ++n){
						int inval = inData[i+m][j+n];
						if (inval != input.getMissing()){
							if ( !set || inval > max ){
								max = inval;
								set = true;
							}
						}
					}
				}
				if ( set ){
					outData[i][j] = max;
				}
			}
		}
		return output;
	}

	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("dilate");
		
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());
		
		// dilate
		LatLonGrid dilate1 = new DilationFilter(1).dilate(popdensity);
		KmlWriter.write(dilate1, out, "dilate_3", PngWriter.createCoolToWarmColormap());
		LatLonGrid dilate3 = new DilationFilter(3).dilate(popdensity);
		KmlWriter.write(dilate3, out, "dilate_7", PngWriter.createCoolToWarmColormap());
		LatLonGrid dilate5 = new DilationFilter(5).dilate(popdensity);
		KmlWriter.write(dilate5, out, "dilate_11", PngWriter.createCoolToWarmColormap());
	}
}
