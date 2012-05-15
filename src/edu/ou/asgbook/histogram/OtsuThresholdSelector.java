/**
 * 
 */
package edu.ou.asgbook.histogram;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.filters.SimpleThresholder;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Uses Otsu (1979) to select optimal threshold
 * @author v.lakshmanan
 *
 */
public class OtsuThresholdSelector {
	private int optimalThreshold;
	private float[] var;
	
	/**
	 * If x is returned, then values < x are one category
	 * and values >= x are another 
	 */
	public OtsuThresholdSelector(Histogram hist){
		// compute p_i
		float[] prob = hist.calcProb();
		
		// mu_T
		float mu_T = 0;
		for (int i=0; i < hist.getHist().length; ++i){
			mu_T += (i+1) * prob[i];
		}
		
		// find k*
		var = new float[hist.getHist().length];
		int best_k = -1;
		float maxvar = 0;
		float w_k = 0;
		float mu_k = 0;
		for (int k=0; k < hist.getHist().length; ++k){
			w_k += prob[k];
			mu_k += (k+1) * prob[k];
			float denom = w_k * (1-w_k);
			float num = mu_T*w_k - mu_k;
			if ( denom > 0 ){
				var[k] = (num * num) / denom;
				// System.out.println(k + " " + var[k]);
				if ( var[k] > maxvar ){
					maxvar = var[k];
					best_k = k;
				}
			}
		}
		
		// return min value of (k+1)th bin
		optimalThreshold = (hist.getMin() + (best_k+1)* hist.getIncr());
	}
	
	public int getOptimalThreshold(){
		return optimalThreshold;
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File outdir = OutputDirectory.getDefault("otsu");
		
		// read input
		LatLonGrid conus = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA);
		
		// find threshold
		final int MIN = 0;
		final int MAX = 100;
		final int incr = 1;
		Histogram hist = new Histogram(MIN, incr, (MAX-MIN)/incr );
		hist.update(conus);
		// System.out.println(hist);
		OtsuThresholdSelector thresholder = new OtsuThresholdSelector(hist);
		int thresh = thresholder.optimalThreshold;
		System.out.println("Optimal threshold=" + thresh);
		
		// plot histogram and variance
		PrintWriter writer = new PrintWriter(new FileWriter(outdir + "/var.txt"));
		for (int i=0; i < hist.getHist().length; ++i){
			int val = (int) (0.5 + hist.getMin() + (i+0.5)*hist.getIncr());
			writer.println(val + " " + hist.getHist()[i] + " " + thresholder.var[i]);
		}
		writer.close();
		
		// threshold
		SimpleThresholder filter = new SimpleThresholder(thresh);
		LatLonGrid binaryImage = filter.threshold(conus);

		KmlWriter.write(binaryImage, outdir, "highpop", PngWriter.createCoolToWarmColormap());
	}
}
