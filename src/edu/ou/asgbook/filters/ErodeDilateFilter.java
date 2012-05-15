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
 * Carries out paired erosion followed by dilation for denoising
 * @author Valliappa.Lakshmanan
 *
 */
public class ErodeDilateFilter extends MultiFilter {

	public ErodeDilateFilter(int halfSize, int numTimes) {
		super(new SpatialFilter[]{
			new ErosionFilter(halfSize), new DilationFilter(halfSize)
		}, numTimes);
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("dilateerode");
		
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());
		
		// erode
		LatLonGrid erode1 = new ErodeDilateFilter(1,1).filter(popdensity);
		KmlWriter.write(erode1, out, "erodedilate_3_1", PngWriter.createCoolToWarmColormap());
		LatLonGrid erode3 = new ErodeDilateFilter(1,3).filter(popdensity);
		KmlWriter.write(erode3, out, "erodedilate_3_3", PngWriter.createCoolToWarmColormap());
		LatLonGrid erode5 = new ErodeDilateFilter(2,3).filter(popdensity);
		KmlWriter.write(erode5, out, "erodedilate_5_3", PngWriter.createCoolToWarmColormap());
	}
}
