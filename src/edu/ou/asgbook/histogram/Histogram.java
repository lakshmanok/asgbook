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
 * A histogram is an empirical probability distribution.
 * 
 * @author v.lakshmanan
 * 
 */
public class Histogram {
	private int min;
	private int incr;
	private int[] hist;

	/**
	 * Values below min are ignored but the last bin is unbounded.
	 * 
	 * @param min
	 * @param incr
	 * @param nbins
	 */
	public Histogram(int min, int incr, int nbins) {
		super();
		this.min = min;
		this.incr = incr;
		this.hist = new int[nbins];
	}

	public int getMin() {
		return min;
	}

	public int getIncr() {
		return incr;
	}

	public int[] getHist() {
		return hist;
	}

	public void update(LatLonGrid data) {
		final int nrows = data.getNumLat();
		final int ncols = data.getNumLon();
		for (int i = 0; i < nrows; ++i)
			for (int j = 0; j < ncols; ++j) {
				int val = data.getValue(i, j);
				int bin_no = getBinNumber(val, data.getMissing());
				if (bin_no != -1 ){
					hist[bin_no]++;
				}
			}
	}

	public int getCenterValue(int bin_no, int missing){
		if (bin_no < 0){
			return missing;
		}
		return min + bin_no*incr + incr/2;
	}
	
	/** points outside the histogram have bin number of -1 */
	public int getBinNumber(int val, int missing) {
		if (val != missing && val >= min) {
			int bin_no = (val - min) / incr;
			// last bin is unbounded
			if (bin_no >= hist.length)
				bin_no = hist.length - 1;
			return bin_no;
		}
		return -1;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hist.length; ++i) {
			int sval = min + i * incr;
			int eval = sval + incr;
			sb.append(sval);
			sb.append(" ");
			sb.append(eval);
			sb.append(" ");
			sb.append(hist[i]);
			sb.append("\n");
		}
		return sb.toString();
	}

	public float[] calcProb() {
		float[] prob = new float[hist.length];
		int tot = 0;
		for (int i = 0; i < hist.length; ++i) {
			tot += hist[i];
		}
		if (tot > 0) {
			for (int i = 0; i < hist.length; ++i) {
				prob[i] = hist[i] / (float) tot;
			}
		}
		return prob;
	}

	public static void main(String[] args) throws Exception {
		// create output directory
		File outdir = OutputDirectory.getDefault("hist");

		// read input
		LatLonGrid conus = SurfaceAlbedo.read(SurfaceAlbedo.CONUS, 100);

		// find histogram
		final int MIN = 0;
		final int MAX = 30;
		for (int incr = 1; incr < 10; incr += 2) {
			Histogram hist = new Histogram(MIN, incr, (MAX - MIN) / incr);
			hist.update(conus);
			System.out.println("INCR=" + incr + " nbins=" + hist.hist.length);
			System.out.println(hist);
			String filename = outdir.getAbsolutePath() + "/hist_" + incr
					+ ".txt";
			PrintWriter writer = new PrintWriter(new FileWriter(filename));
			writer.println(hist);
			writer.close();
			System.out.println("Wrote to " + filename);
		}
	}

	public int getNumBins() {
		return hist.length;
	}
}
