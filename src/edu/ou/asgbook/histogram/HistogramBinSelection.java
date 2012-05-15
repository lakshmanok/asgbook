/**
 * 
 */
package edu.ou.asgbook.histogram;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.ScalarStatistic;
import edu.ou.asgbook.dataset.SurfaceAlbedo;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * 
 * Tries out different values for the number of bins and replaces
 * each pixel value by the center of its bin.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class HistogramBinSelection {
	/** replaces each pixel by the center of its bin */
	public static LatLonGrid band(LatLonGrid data, Histogram hist){
		LatLonGrid result = LatLonGrid.copyOf(data);
		int nrows = result.getNumLat();
		int ncols = result.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int bin_no = hist.getBinNumber(data.getValue(i,j), data.getMissing());
			int cval = hist.getCenterValue(bin_no, data.getMissing());
			result.setValue(i,j, cval);
		}
		return result;
	}
	
	/** Based on range. */
	public static Histogram createBasedOnRange(LatLonGrid data){
		// find the range
		int min = data.getMissing();
		int max = data.getMissing();
		int nrows = data.getNumLat();
		int ncols = data.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int val = data.getValue(i,j);
			if ( val != data.getMissing() ){
				if (min == data.getMissing() || val < min){
					min = val;
				}
				if (max == data.getMissing() || val > max){
					max = val;
				}
			}
		}
		int nbins = 1 + (int) Math.round(Math.log(max-min)/Math.log(2));
		System.out.println("Based on range: min=" + min + " max="+ max + ", nbins=" + nbins);
		int incr = (max-min)/nbins;
		if (incr == 0) incr = 1;
		Histogram hist = new Histogram(min,incr,nbins);
		hist.update(data);
		return hist;
	}
	
	/** Highest resolution possible. */
	public static Histogram createHighestResolution(LatLonGrid data){
		// find the range
		int min = data.getMissing();
		int max = data.getMissing();
		int nrows = data.getNumLat();
		int ncols = data.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int val = data.getValue(i,j);
			if ( val != data.getMissing() ){
				if (min == data.getMissing() || val < min){
					min = val;
				}
				if (max == data.getMissing() || val > max){
					max = val;
				}
			}
		}
		System.out.println("full resolution: min=" + min + " max="+ max + ", incr=1");
		final int incr = 1;
		int nbins = (max-min)+1;
		Histogram hist = new Histogram(min,incr,nbins);
		hist.update(data);
		return hist;
	}
	
	/** Based on range. */
	public static Histogram createBasedOnStdDev(LatLonGrid data){
		ScalarStatistic stat = new ScalarStatistic();
		int min = data.getMissing();
		int max = data.getMissing();
		int nrows = data.getNumLat();
		int ncols = data.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int val = data.getValue(i,j);
			if ( val != data.getMissing() ){
				stat.update(val);
				if (min == data.getMissing() || val < min){
					min = val;
				}
				if (max == data.getMissing() || val > max){
					max = val;
				}
			}
		}
		double sigma = stat.getStdDeviation();
		int N = stat.getNumSamples();
		int nbins = 1 + (int) Math.round(3.5*sigma/Math.pow(N, 1.0/3));
		System.out.println("Based on sigma=" + sigma + " N="+ N + ", nbins=" + nbins);
		int incr = (max-min)/nbins;
		if (incr == 0) incr = 1;
		Histogram hist = new Histogram(min,incr,nbins);
		hist.update(data);
		return hist;
	}
	
	/** Based on range. */
	public static Histogram createBasedOnNumSamples(LatLonGrid data){
		int min = data.getMissing();
		int max = data.getMissing();
		int N = 0;
		int nrows = data.getNumLat();
		int ncols = data.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int val = data.getValue(i,j);
			if ( val != data.getMissing() ){
				++N;
				if (min == data.getMissing() || val < min){
					min = val;
				}
				if (max == data.getMissing() || val > max){
					max = val;
				}
			}
		}
		int nbins = 1 + (int) Math.round(Math.sqrt(N));
		System.out.println("Based on N="+ N + ", nbins=" + nbins);
		int incr = (max-min)/nbins;
		if (incr == 0) incr = 1;
		Histogram hist = new Histogram(min,incr,nbins);
		hist.update(data);
		return hist;
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File outdir = OutputDirectory.getDefault("histbin");

		// read input
		LatLonGrid conus = SurfaceAlbedo.read(SurfaceAlbedo.CONUS, 100);

		// find histogram in three different ways
		Map<String, Histogram> map = new HashMap<String,Histogram>();
		map.put("range", HistogramBinSelection.createBasedOnRange(conus));
		map.put("numsamples", HistogramBinSelection.createBasedOnNumSamples(conus));
		map.put("stddev", HistogramBinSelection.createBasedOnStdDev(conus));

		for (Map.Entry<String, Histogram> entry : map.entrySet()) {
			Histogram hist = entry.getValue();
			String name = entry.getKey();
			
			String filename = outdir.getAbsolutePath() + "/hist_" + name
					+ ".txt";
			PrintWriter writer = new PrintWriter(new FileWriter(filename));
			writer.println(hist);
			writer.close();
			System.out.println("Wrote to " + filename);
			
			LatLonGrid banded = HistogramBinSelection.band(conus, hist);
			KmlWriter.write(banded, outdir, name, PngWriter.createCoolToWarmColormap());
		}
	}
}
