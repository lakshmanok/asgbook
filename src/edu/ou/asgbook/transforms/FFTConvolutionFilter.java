package edu.ou.asgbook.transforms;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.filters.ConvolutionFilter;
import edu.ou.asgbook.filters.SpatialFilter;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.transforms.FFT.Complex;

/**
 * An optimization for convolution using FFTs.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class FFTConvolutionFilter extends FFT2D implements SpatialFilter {
	private double[][] coeffs;
	
	public FFTConvolutionFilter(double[][] coeffs){
		this.coeffs = coeffs;
	}
	
	public LatLonGrid convolve(LatLonGrid data){
		Complex[][] in1 = zeropad(data);
		int nrows = in1.length;
		int ncols = in1[0].length;
		Complex[][] in2 = zeropad(coeffs, nrows, ncols );
		
		// compute their ffts
		in1 = fft(in1);
		in2 = fft(in2);
		
		// multiply point by point (this by the conjugate of other)
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			in1[i][j] = in1[i][j].multiply(in2[i][j].conjugate());
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
	
	@Override
	public LatLonGrid filter(LatLonGrid input) {
		return convolve(input);
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("fftconv");
		
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(500, 1000, 1024, 2048);
		System.out.println("Grid size=" + popdensity.getNumLat() + "x" + popdensity.getNumLon());
		KmlWriter.write(popdensity, out, "fullgrid", PngWriter.createCoolToWarmColormap());
		double[][] coeffs = ConvolutionFilter.gauss(301, 301);
		long timer = System.currentTimeMillis();
		LatLonGrid sm = new FFTConvolutionFilter(coeffs).convolve(popdensity);
		long ffttime = System.currentTimeMillis() - timer;
		KmlWriter.write(sm, out, "fftgauss", PngWriter.createCoolToWarmColormap());
		
		// do it in spatial domain
		timer = System.currentTimeMillis();
		LatLonGrid sm2 = new ConvolutionFilter(coeffs).convolve(popdensity);
		long spatialtime = System.currentTimeMillis() - timer;
		KmlWriter.write(sm2, out, "spgauss", PngWriter.createCoolToWarmColormap());
		
		double improvement = 100*((double)(spatialtime - ffttime))/spatialtime;
		System.out.println("The FFT technique took " + ffttime + "ms whereas the spatial technique took " + spatialtime + " ms. FFT is " + improvement + "% faster");
	}

}
