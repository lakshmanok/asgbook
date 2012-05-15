/**
 * 
 */
package edu.ou.asgbook.filters;

import java.io.File;
import java.text.DecimalFormat;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Convolve an image by a window that is akin to the features we want to extract.
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class MatchedFilter implements SpatialFilter {
	private double[][] coeffs;
	
	public MatchedFilter(double[][] coeffs) {
		this.coeffs = coeffs;
		if ( coeffs.length % 2 == 0 || coeffs[0].length % 2 == 0 ){
			throw new IllegalArgumentException("Dimensions of coefficients array needs to be odd");
		}
		// normalize
		double sum = 0;
		for (int i=0; i < coeffs.length; ++i){
			for (int j=0; j < coeffs[i].length; ++j){
				sum += coeffs[i][j];
			}
		}
		System.out.println("Normalizing coefficients by " + sum);
		DecimalFormat df = new DecimalFormat("0.000");
		for (int i=0; i < coeffs.length; ++i){
			for (int j=0; j < coeffs[i].length; ++j){
				coeffs[i][j] /= sum;
				System.out.print(df.format(coeffs[i][j]) + "&"); // for LaTeX
			}
			System.out.println("\\\\");
		}		
	}
	
	/**
	 * returns a grid with values in the range 0-100
	 */
	public LatLonGrid match(final LatLonGrid input){
		LatLonGrid output = LatLonGrid.copyOf(input);
		output.setMissing(-1);
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
				int totval = 0; // normalize values in window
				for (int m=-hx; m <= hx; ++m){
					for (int n=-hy; n <= hy; ++n){
						double coeff = coeffs[m+hx][n+hy];
						int inval = inData[i+m][j+n];
						if (inval != input.getMissing()){
							tot += inval*coeff;
							totval += inval;
						}
					}
				}
				if (totval != 0){
					outData[i][j] = (int) Math.round(10000 * tot / totval);
				}
			}
		}
		return output;
	}
	

	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("matched");
		
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());
		
		for (int hx=1; hx <= 5; hx += 2){
			int hy = hx*2;
			double[][] coeffs = new double[2*hx+1][2*hy+1];
			for (int i=0; i < coeffs.length; ++i){
				for (int j=0; j < coeffs[i].length; ++j){
					int t = i + j;
					if ( t < coeffs.length ) coeffs[i][j] = 1;
				}
			}
			MatchedFilter filter = new MatchedFilter(coeffs);
			LatLonGrid sm = filter.match(popdensity);
			KmlWriter.write(sm, out, "northwest"+hx, PngWriter.createCoolToWarmColormap());
		}
		
		for (int hx=5; hx < 15; hx += 3){
			// int hx = 8;
			int hy = hx;
			double[][] coeffs = new double[2*hx+1][2*hy+1];
			for (int i=0; i < coeffs.length; ++i){
				for (int j=0; j < coeffs[i].length; ++j){
					int dx = i - hx;
					int dy = j - hy;
					if ( Math.abs(dx) < hx/2 && Math.abs(dy) < hy/2 )
						coeffs[i][j] = 1;
				}
			}
			MatchedFilter filter = new MatchedFilter(coeffs);
			LatLonGrid sm = filter.match(popdensity);
			KmlWriter.write(sm, out, "isolated"+hx, PngWriter.createCoolToWarmColormap());
		}
	}

	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return match(input);
	}
}
