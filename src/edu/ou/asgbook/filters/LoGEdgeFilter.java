package edu.ou.asgbook.filters;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.linearity.DataTransform;

/**
 * Laplacian of a Gaussian edge filter.
 * @author valliappa.lakshmanan
 *
 */
public class LoGEdgeFilter implements SpatialFilter {
	private final ConvolutionFilter log;
	private final int thresh;
	
	public LoGEdgeFilter(int halfsize, int edgethresh){
		double sigma = halfsize/3.0;
		double[][] coeffs = new double[2*halfsize+1][2*halfsize+1];
		double tot = 0;
		for (int x=-halfsize; x <= halfsize; ++x){
			for (int y=-halfsize; y <= halfsize; ++y){
				double term1 = (x*x + y*y - sigma*sigma)/Math.pow(sigma,4);
				double term2 = Math.exp(-(x*x + y*y)/(2*sigma*sigma));
				double coeff = term1 * term2;
				coeffs[x+halfsize][y+halfsize] = coeff;
				tot += coeff;
			}
		}
		// ensure that coeffs add up to zero
		coeffs[halfsize][halfsize] -= tot;
		this.log = new ConvolutionFilter(coeffs);
		this.thresh = edgethresh;
	}
	
	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return edgeFilter(input);
	}
	
	public LatLonGrid edgeFilter(final LatLonGrid input){
		return edgeFilter(input, null);
	}
	
	public LatLonGrid edgeFilter(final LatLonGrid input, File out){
		// Laplacian of a Gaussian
		LatLonGrid dgg = log.convolve(input);
		KmlWriter.debugWrite(dgg, out, "laplacianofgaussian");
		int bound = log.getFilterNumRows();
		// find zero crossings in 3x3 neighborhood
		int nrows = dgg.getNumLat();
		int ncols = dgg.getNumLon();
		LatLonGrid result = new LatLonGrid(nrows,ncols,-1,dgg.getNwCorner(),dgg.getLatRes(),dgg.getLonRes());
		for (int i=bound; i < (nrows-bound); ++i) for (int j=bound; j < (ncols-bound); ++j){
			int mag = 0;
			// ver
			mag = checkZeroCrossing(dgg.getValue(i-1,j), dgg.getValue(i+1,j), mag);
			// hor
			mag = checkZeroCrossing(dgg.getValue(i,j-1), dgg.getValue(i,j+1), mag);
			// diag1
			mag = checkZeroCrossing(dgg.getValue(i-1,j-1), dgg.getValue(i+1,j+1), mag);
			// diag2
			mag = checkZeroCrossing(dgg.getValue(i+1,j-1), dgg.getValue(i-1,j+1), mag);
			// mag is over 2 bins
			mag /= 2;
			if ( mag > thresh ){
				result.setValue(i,j, mag);
			}
		}
		return result;
	}
	
	private int checkZeroCrossing(int a, int b, int mag) {
		if (a*b < 0){
			return Math.max( mag, Math.abs(a-b) );
		}
		return mag;
	}

	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("logedge");
		
		// read input
		DataTransform t = new GlobalPopulation.LogScaling();
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, t).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());
				
		LoGEdgeFilter filter = new LoGEdgeFilter(5,400);
		LatLonGrid edges = filter.edgeFilter(popdensity, out);
		KmlWriter.write(edges, out, "logedge", PngWriter.createCoolToWarmColormap());
	}
}
