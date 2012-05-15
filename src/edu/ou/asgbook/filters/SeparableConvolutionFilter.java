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
 * An optimized convolution filter
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class SeparableConvolutionFilter implements SpatialFilter {
	private double[] coeffs_x;
	private double[] coeffs_y;
	
	public SeparableConvolutionFilter(double[] coeffs_x, double[] coeffs_y) {
		this.coeffs_x = coeffs_x;
		if ( coeffs_x.length % 2 == 0 ){
			throw new IllegalArgumentException("Dimensions of coefficients array needs to be odd");
		}
		this.coeffs_y = coeffs_y;
		if ( coeffs_y.length % 2 == 0 ){
			throw new IllegalArgumentException("Dimensions of coefficients array needs to be odd");
		}
	}
	
	public SeparableConvolutionFilter(double[] coeffs) {
		this(coeffs,coeffs);
	}

	public LatLonGrid smooth(final LatLonGrid input){
		int[][] inData = input.getData();
		final int nx = input.getNumLat();
		final int ny = input.getNumLon();
		
		// filter the rows
		final int hx = coeffs_x.length / 2;
		int[][] rowResult = new int[nx][ny];
		for (int j=0; j < ny; ++j){
			for (int i=hx; i < (nx-hx); ++i){
				double tot = 0;
				double wt = 0;
				for (int m=-hx; m <= hx; ++m){
					double coeff = coeffs_x[m+hx];
					int inval = inData[i+m][j];
					if (inval != input.getMissing()){
						tot += inval*coeff;
						wt += coeff;
					}
				}
				if ( wt > 0 ){
					rowResult[i][j] = (int)( Math.round(tot / wt) );
				}
			}
		}
		
		// now filter the columns of rowResult
		inData = rowResult;
		LatLonGrid output = LatLonGrid.copyOf(input);
		output.fill(output.getMissing());
		final int hy = coeffs_y.length / 2;
		int[][] outData = output.getData();
		for (int i=0; i < nx; ++i){
			for (int j=hy; j < (ny-hy); ++j){
				double tot = 0;
				double wt = 0;
				for (int n=-hy; n <= hy; ++n){
					double coeff = coeffs_y[n+hy];
					int inval = inData[i][j+n];
					if (inval != input.getMissing()){
						tot += inval*coeff;
						wt += coeff;
					}
				}
				if ( wt > 0 ){
					outData[i][j] = (int)( Math.round(tot / wt) );
				}
			}
		}		
		
		return output;
	}
	
	public static SeparableConvolutionFilter boxcar(int numx, int numy){
		double[] coeffs_x = new double[numx];
		double[] coeffs_y = new double[numy];
		for (int i=0; i < numx; ++i){
			coeffs_x[i] = 1.0/numx;
		}
		for (int i=0; i < numy; ++i){
			coeffs_y[i] = 1.0/numy;
		}
		return new SeparableConvolutionFilter(coeffs_x, coeffs_y);
	}
	
	public static SeparableConvolutionFilter gauss(int numx, int numy){
		return new SeparableConvolutionFilter(gauss(numx), gauss(numy));
	}
	
	public static double[] gauss(int numx){
		double[] coeffs = new double[numx];
		double sigmax = numx / 6.0; // 3-sigma on either side
		for (int i=0; i < coeffs.length; ++i){
			double x = (i - coeffs.length/2.0)/sigmax;
			coeffs[i] = Math.exp(-(x*x));
		}
		return coeffs;
	}

	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("separable");
		
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());
		
		// boxcar
		{
			SeparableConvolutionFilter filter = SeparableConvolutionFilter.boxcar(5, 5);
			LatLonGrid sm = filter.smooth(popdensity);
			KmlWriter.write(sm, out, "boxcar", PngWriter.createCoolToWarmColormap());
		}
		
		// gauss
		{
			SeparableConvolutionFilter filter = SeparableConvolutionFilter.gauss(11, 11);
			LatLonGrid sm = filter.smooth(popdensity);
			KmlWriter.write(sm, out, "gauss", PngWriter.createCoolToWarmColormap());
		}
	}

	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return smooth(input);
	}
}
