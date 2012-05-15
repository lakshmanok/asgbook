package edu.ou.asgbook.transforms;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.filters.SpatialFilter;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.transforms.FFT.Complex;

/**
 * Removes noise (high frequencies) and the gross signal (low frequencies).
 * 
 * @author valliappa.lakshmanan
 *
 */
public class FFTBandpassFilter extends FFT2D implements SpatialFilter {
	private double minr, maxr;
	
	/**
	 * Supply numbers in the range (0,1) where 1 is the full dynamic range
	 */
	public FFTBandpassFilter(double minr, double maxr){
		this.minr = minr;
		this.maxr = maxr;
	}
	
	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return convolve(input);
	}
	
	public LatLonGrid convolve(LatLonGrid data){
		Complex[][] in1 = zeropad(data);
		final int nrows = in1.length;
		final int ncols = in1[0].length;

		// compute the fft
		in1 = fft(in1);
		
		// the fft is arranged in quadrants, so we need to be careful
		// to remove the corresponding data in all the quadrants
		Complex zero = new Complex(0,0);
		double diag = Math.sqrt(nrows*nrows+ncols*ncols)/4;
		for (int i=0; i < nrows/2; ++i){
			for (int j=0; j < ncols/2; ++j){
				double r = Math.sqrt(i*i + j*j)/diag;
				if (r < minr || r > maxr){
					in1[i][j] = zero; // 1st quadrant
					in1[nrows-i-1][j] = zero; // 3rd quadrant
					in1[i][ncols-j-1] = zero; // 2nd quandrant
					in1[nrows-i-1][ncols-i-1] = zero; // 4th quadrant
				}
			}
		}
		
		// take ifft
		Complex[][] result = ifft(in1);
		
		// return real part, rounded off
		LatLonGrid out = LatLonGrid.copyOf(data);
		for (int i=0; i < out.getNumLat(); ++i) for (int j=0; j < out.getNumLon(); ++j){
			out.setValue(i,j, (int)Math.round(result[i][j].real));
		}
		return out;
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("fftbandpass");
		
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());
		LatLonGrid sm = new FFTBandpassFilter(0,0.1).convolve(popdensity);
		KmlWriter.write(sm, out, "bp0_10", PngWriter.createCoolToWarmColormap());
		sm = new FFTBandpassFilter(0,0.2).convolve(popdensity);
		KmlWriter.write(sm, out, "bp0_20", PngWriter.createCoolToWarmColormap());
		sm = new FFTBandpassFilter(0.2,0.8).convolve(popdensity);
		KmlWriter.write(sm, out, "bp20_80", PngWriter.createCoolToWarmColormap());
		sm = new FFTBandpassFilter(0.8,1.0).convolve(popdensity);
		KmlWriter.write(sm, out, "bp80_100", PngWriter.createCoolToWarmColormap());
	}

}
