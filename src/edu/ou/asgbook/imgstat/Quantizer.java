/**
 * 
 */
package edu.ou.asgbook.imgstat;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.SurfaceAlbedo;
import edu.ou.asgbook.histogram.Histogram;
import edu.ou.asgbook.histogram.HistogramBinSelection;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Develops a quantization scheme using histogram equalization
 * 
 * @author valliappa.lakshmanan
 *
 */
public class Quantizer {
	private int min;
	private int[] upperBound;
	/**
	 * Pass in a high-resolution histogram i.e. with incr=1, for example
	 * @param hist
	 * @param K  number of levels
	 */
	public Quantizer(Histogram hist, int K){
		this.min = hist.getMin();
		int incr = hist.getIncr();
		int[] freq = hist.getHist();
		int N = 0; // number of samples
		for (int i=0; i < freq.length; ++i){
			N += freq[i];
		}
		double N_per_level = N/(double)K;
		
		// populate
		upperBound = new int[K];
		int level_no=0;
		int at_this_level = 0;
		for (int bin_no=0; bin_no < freq.length; ++bin_no){
			if (at_this_level < N_per_level){
				at_this_level += freq[bin_no]; // on to next
			} else {
				upperBound[level_no] = min + (bin_no * incr);
				// next level
				++level_no;
				at_this_level = freq[bin_no];
			}
		}
		for (; level_no < K; ++level_no){
			upperBound[level_no] = min + freq.length * incr;
		}
		System.out.println(this);
	}
	
	public int getBinNumber(int val){
		for (int i=0; i < upperBound.length; ++i){
			if (val < upperBound[i]){
				return i;
			}
		}
		return -1;
	}
	
	public int getCenterValue(int bin_no){
		int lb = (bin_no > 0)? upperBound[bin_no-1] : this.min;
		int ub = upperBound[bin_no];
		return (ub+lb)/2;
	}
	
	/** replaces each pixel by the center of its bin */
	public LatLonGrid band(LatLonGrid data){
		LatLonGrid result = LatLonGrid.copyOf(data);
		int nrows = result.getNumLat();
		int ncols = result.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int bin_no = getBinNumber(data.getValue(i,j));
			int cval = (bin_no < 0)? data.getMissing() : getCenterValue(bin_no);
			result.setValue(i,j, cval);
		}
		return result;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Quantizer levels: ");
		for (int i=0; i < upperBound.length; ++i){
			sb.append(upperBound[i]);
			sb.append(" ");
		}
		return sb.toString();
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File outdir = OutputDirectory.getDefault("quantizer");

		// read input
		LatLonGrid conus = SurfaceAlbedo.read(SurfaceAlbedo.CONUS, 100);

		// find histogram in three different ways
		Histogram full = HistogramBinSelection.createHighestResolution(conus);
		for (int k=4; k < 32; k *= 2){ // 4, 8, 16
			Quantizer quant = new Quantizer(full, k);
			LatLonGrid banded = quant.band(conus);
			KmlWriter.write(banded, outdir, "quant_" + k, PngWriter.createCoolToWarmColormap());
			
			int incr = (int) Math.round( full.getIncr() * full.getHist().length / (double) k);
			Histogram eq = new Histogram(full.getMin(),incr, k);
			banded = HistogramBinSelection.band(conus, eq);
			KmlWriter.write(banded, outdir, "hist_" + k, PngWriter.createCoolToWarmColormap());
		}
	}
}
