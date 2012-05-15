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
 * Convolve an image by a window.
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class ConvolutionFilter implements SpatialFilter {
	private double[][] coeffs;
	
	public ConvolutionFilter(double[][] coeffs) {
		this.coeffs = coeffs;
		if ( coeffs.length % 2 == 0 || coeffs[0].length % 2 == 0 ){
			throw new IllegalArgumentException("Dimensions of coefficients array needs to be odd");
		}
	}

	public int getFilterNumRows(){
		return coeffs.length;
	}
	
	public int getFilterNumCols(){
		return coeffs[0].length;
	}
	
	/**
	 * Uses weights, but only at non-missing pixels, and divides by the total weight.
	 * Use this for smoothing
	 */
	public LatLonGrid smooth(final LatLonGrid input){
		LatLonGrid output = LatLonGrid.copyOf(input);
		output.fill(output.getMissing());
		int[][] outData = output.getData();
		int[][] inData = input.getData();
		final int hx = coeffs.length / 2;
		final int hy = coeffs[0].length / 2;
		final int nx = output.getNumLat();
		final int ny = output.getNumLon();
		for (int i=hx; i < (nx-hx); ++i){
			for (int j=hy; j < (ny-hy); ++j){
				double tot = 0;
				double wt = 0;
				for (int m=-hx; m <= hx; ++m){
					for (int n=-hy; n <= hy; ++n){
						double coeff = coeffs[m+hx][n+hy];
						int inval = inData[i+m][j+n];
						if (inval != input.getMissing()){
							tot += inval*coeff;
							wt += coeff;
						}
					}
				}
				if ( wt > 0 ){
					outData[i][j] = (int)( Math.round(tot / wt) );
				}
			}
		}
		return output;
	}
	
	/**
	 * Uses the coefficients and returns the convolved value without dividing by sum of weights
	 * Use this for non-smoothing coefficients.
	 */
	public LatLonGrid convolve(final LatLonGrid input){
		LatLonGrid output = LatLonGrid.copyOf(input);
		output.fill(output.getMissing());
		int[][] outData = output.getData();
		int[][] inData = input.getData();
		final int hx = coeffs.length / 2;
		final int hy = coeffs[0].length / 2;
		final int nx = output.getNumLat();
		final int ny = output.getNumLon();
		for (int i=hx; i < (nx-hx); ++i){
			for (int j=hy; j < (ny-hy); ++j){
				double tot = 0;
				for (int m=-hx; m <= hx; ++m){
					for (int n=-hy; n <= hy; ++n){
						double coeff = coeffs[m+hx][n+hy];
						int inval = inData[i+m][j+n];
						if (inval != input.getMissing()){
							tot += inval*coeff;
						}
					}
				}
				outData[i][j] = (int) Math.round(tot);
			}
		}
		return output;
	}
	
	public static double[][] boxcar(int numx, int numy){
		double[][] coeffs = new double[numx][numy];
		
		double tot = numx * numy;
		for (int i=0; i < coeffs.length; ++i){
			for (int j=0; j < coeffs.length; ++j){
				coeffs[i][j] = 1 / tot;
			}
		}
		
		return coeffs;
	}
	
	public static double[][] gauss(int numx, int numy){
		return gauss(numx, numy, numx/6.0, numy/6.0); // 3-sigma on either side
	}
	
	public static double[][] gauss(int numx, int numy, double sigmax, double sigmay){
		double[][] coeffs = new double[numx][numy];
		
		for (int i=0; i < coeffs.length; ++i){
			for (int j=0; j < coeffs.length; ++j){
				double x = (i - coeffs.length/2.0)/sigmax;
				double y = (j - coeffs[0].length/2.0)/sigmay;
				coeffs[i][j] = Math.exp(-(x*x + y*y));
			}
		}
		
		return coeffs;
	}

	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("convolve");
		
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());
		
		// average
		{
			ConvolutionFilter filter = new ConvolutionFilter(ConvolutionFilter.boxcar(3, 3));
			LatLonGrid sm = filter.smooth(popdensity);
			KmlWriter.write(sm, out, "boxcar1", PngWriter.createCoolToWarmColormap());
		}
		
		// boxcar
		{
			ConvolutionFilter filter = new ConvolutionFilter(ConvolutionFilter.boxcar(5, 5));
			LatLonGrid sm = filter.smooth(popdensity);
			KmlWriter.write(sm, out, "boxcar", PngWriter.createCoolToWarmColormap());
		}
		
		// gauss
		{
			ConvolutionFilter filter = new ConvolutionFilter(ConvolutionFilter.gauss(11, 11));
			LatLonGrid sm = filter.smooth(popdensity);
			KmlWriter.write(sm, out, "gauss", PngWriter.createCoolToWarmColormap());
		}
	}

	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return convolve(input);
	}
}
