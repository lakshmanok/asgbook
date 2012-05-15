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
 * Reduces the size of entities by taking a local mininum.
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class ErosionFilter implements SpatialFilter {
	private int halfSize;
	
	public ErosionFilter(int halfSize) {
		this.halfSize = halfSize;
	}
	
	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return erode(input);
	}

	public LatLonGrid erode(final LatLonGrid input){
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
				int min = input.getMissing();
				boolean set = false;
				for (int m=-hx; m <= hx; ++m){
					for (int n=-hy; n <= hy; ++n){
						int inval = inData[i+m][j+n];
						if (inval != input.getMissing()){
							if ( !set || inval < min ){
								min = inval;
								set = true;
							}
						}
					}
				}
				if ( set ){
					outData[i][j] = min;
				}
			}
		}
		return output;
	}

	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("erode");
		
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());
		
		// erode
		LatLonGrid erode1 = new ErosionFilter(1).erode(popdensity);
		KmlWriter.write(erode1, out, "erode_3", PngWriter.createCoolToWarmColormap());
		LatLonGrid erode3 = new ErosionFilter(3).erode(popdensity);
		KmlWriter.write(erode3, out, "erode_7", PngWriter.createCoolToWarmColormap());
		LatLonGrid erode5 = new ErosionFilter(5).erode(popdensity);
		KmlWriter.write(erode5, out, "erode_11", PngWriter.createCoolToWarmColormap());
	}
}
