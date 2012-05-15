/**
 * 
 */
package edu.ou.asgbook.histogram;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.SurfaceAlbedo;
import edu.ou.asgbook.io.OutputDirectory;

/**
 * Forms a CDF from a Histogram
 * @author v.lakshmanan
 *
 */
public class CumulativeDistributionFunction {
	private int min;
	private int incr;
	private float[] prob;
	
	public CumulativeDistributionFunction(Histogram hist){
		this.min = hist.getMin();
		this.incr = hist.getIncr();
		prob = new float[hist.getHist().length];
		int tot = 0;
		for (int i=0; i < hist.getHist().length; ++i){
			tot += hist.getHist()[i];
		}
		if ( tot == 0 ) return;
		
		int sofar = 0;
		for (int i=0; i < hist.getHist().length; ++i){
			sofar += hist.getHist()[i];
			prob[i] = sofar / (float) tot;
		}
	}

	public int getMin() {
		return min;
	}

	public int getIncr() {
		return incr;
	}

	public float[] getProb() {
		return prob;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < prob.length; ++i){
			int sval = min + i * incr;
			int eval = sval + incr;
			sb.append(sval);
			sb.append(" ");
			sb.append(eval);
			sb.append(" ");
			sb.append(prob[i]);
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File outdir = OutputDirectory.getDefault("cdf");
		
		// read input
		LatLonGrid conus = SurfaceAlbedo.read(SurfaceAlbedo.CONUS, 100);
		
		// find histogram
		final int MIN = 0;
		final int MAX = 30;
		for (int incr=1; incr < 10; incr += 2){
			Histogram hist = new Histogram(MIN, incr, (MAX-MIN)/incr );
			hist.update(conus);
			CumulativeDistributionFunction cdf = new CumulativeDistributionFunction(hist);
			System.out.println("INCR=" + incr + " nbins=" + cdf.prob.length);
			System.out.println(cdf);
			String filename = outdir.getAbsolutePath() + "/cdf_" + incr + ".txt";
			PrintWriter writer = new PrintWriter(new FileWriter(filename));
			writer.println(cdf);
			writer.close();
			System.out.println("Wrote to " + filename);
		}
	}
}
