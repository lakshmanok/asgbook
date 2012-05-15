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
 * A non-isotropic smoothing filter.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class OrientedEllipseFilter implements SpatialFilter {
	private ConvolutionFilter[] filterBank;
	
	public OrientedEllipseFilter(int numFilters, int a, int b){
		if (a == b){
			throw new IllegalArgumentException("For an ellipse, a != b");
		}		
		filterBank = new ConvolutionFilter[numFilters];
		int size = Math.max(a,b) * 2 + 1;
		for (int f=0; f < numFilters; ++f){
			double[][] coeffs = new double[size][size];
			double theta = (f * Math.PI) / numFilters; // 0 to 180
			for (int i=0; i < size; ++i) for (int j=0; j < size; ++j){
				double x = i - a;
				double y = j - b;
				double term1 = x*Math.cos(theta) - y*Math.sin(theta);
				term1 = (term1*term1) / (a*a);
				double term2 = x*Math.sin(theta) + y*Math.cos(theta);
				term2 = (term2*term2) / (b*b);
				if ( (term1+term2) <= 1 ){
					coeffs[i][j] = 1;
				} // else zero
			}
			filterBank[f] = new ConvolutionFilter(coeffs);
		}
	}
	
	/**
	 * Finds the maximum response of all the oriented filters.
	 */
	public LatLonGrid smooth(LatLonGrid input, File out){
		LatLonGrid result = filterBank[0].smooth(input);
		KmlWriter.debugWrite(result, out, "ellipse0");
		for (int f=1; f < filterBank.length; ++f){
			LatLonGrid fth = filterBank[f].smooth(input);
			KmlWriter.debugWrite(fth, out, "ellipse"+f);
			int nrows = fth.getNumLat();
			int ncols = fth.getNumLon();
			for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
				int maxval = result.getValue(i,j);
				int fthval = fth.getValue(i,j);
				if (maxval == input.getMissing() ||
					(fthval != input.getMissing() && fthval > maxval) ){
					maxval = fthval;
				}
				result.setValue(i,j, maxval);
			}
		}
		return result;
	}
	
	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return smooth(input, null);
	}

	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("oriented");
		
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());
		
		OrientedEllipseFilter filter = new OrientedEllipseFilter(8, 1, 5);
		LatLonGrid sm = filter.smooth(popdensity, out);
		KmlWriter.write(sm, out, "ellipse", PngWriter.createCoolToWarmColormap());
	}
}
