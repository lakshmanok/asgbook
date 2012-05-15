/**
 * 
 */
package edu.ou.asgbook.motion;

import java.io.File;
import java.util.Date;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pair;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.dataset.SeviriInfraredTemperature;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Estimates motion using cross-correlation.
 * 
 * @author v.lakshmanan
 *
 */
public class CrossCorrelation implements MotionEstimator {
	private final int EST_HALFSIZE_NS; // size of estimation window
	private final int EST_HALFSIZE_EW;
	private final int MAX_U;
	private final int MAX_V;
	
	private final double MAX_ERROR_RATIO = 0.75; // maximum of 75% change
	private final double MIN_FILL_RATIO  = 0.5; // at least 50% filled
	private final int MIN_FILL_PIXELS;
	
	/**
	 * Pass size of window to estimate motion of, and the maximum movement in the two directions.
	 * @param est_halfsize_x
	 * @param est_halfsize_y
	 */
	
	public CrossCorrelation(int est_halfsize_x, int est_halfsize_y,
			int maxmotion_x, int maxmotion_y) {
		super();
		EST_HALFSIZE_NS = est_halfsize_x;
		EST_HALFSIZE_EW = est_halfsize_y;
		MAX_U = maxmotion_x;
		MAX_V = maxmotion_y;
		MIN_FILL_PIXELS = (int) Math.round(MIN_FILL_RATIO * (2*EST_HALFSIZE_NS+1) * (2*EST_HALFSIZE_EW+1));
	}

	@Override
	public Pair<LatLonGrid, LatLonGrid> compute(LatLonGrid data0, LatLonGrid data1, File outdir){
		// Grids we need. initialize all of them at zero
		final int nrows = data1.getNumLat();
		final int ncols = data1.getNumLon();
		LatLonGrid u = new LatLonGrid(nrows, ncols, 0, data1.getNwCorner(), data1.getLatRes(), data1.getLonRes());
		LatLonGrid v = LatLonGrid.copyOf(u);
		
		System.out.println("Computing u,v using xcorr on " + nrows + "x" + ncols + " image; search radius=" + MAX_U + "x" + MAX_V);
		
		// compute u,v for every pixel
		double meanu = 0;
		double meanv = 0;
		int nestimates = 0;
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			// at pixel, search best match for entire neighborhood
			// best_m, best_n are not changed from default unless < error_ratio
			double lse = MAX_ERROR_RATIO;
			int best_m = 0;
			int best_n = 0;
			for (int m=-MAX_U; m <= MAX_U; ++m){
				for (int n=-MAX_V; n <= MAX_V; ++n){
					double error = compute_error(data0, data1, i, j, m, n);
					if ( error < lse ){
						lse = error;
						best_m = m;
						best_n = n;
					}
				}
			}
			u.setValue(i,j, best_m);
			v.setValue(i,j, best_n);
			
			if ( lse != MAX_ERROR_RATIO ){
				meanu += best_m;
				meanv += best_n;
				++nestimates;
			}
			
			if ( i%10 == 0 && j == 0){
				System.out.println( (100*i)/nrows + "% of pixels complete.");
			}
		}
		
		System.out.println("Mean motion vector: u=" + meanu/(nestimates) + " v=" + meanv/(nestimates));
		return new Pair<LatLonGrid,LatLonGrid>(u,v);
	}
	
	private double compute_error(LatLonGrid grid0, LatLonGrid grid1, int x, int y, int u, int v) {
		// take the pixel at data1 and move it backwards by u,v and check error
		int tot_error = 0;
		int tot_data = 0;
		int num_data_valid = 0;
		for (int i=x-EST_HALFSIZE_NS; i <= x+EST_HALFSIZE_NS; ++i){
			for (int j=y-EST_HALFSIZE_EW; j <= y+EST_HALFSIZE_EW; ++j){
				boolean data0ok = grid0.isValid(i-u,j-v) && grid0.getValue(i-u,j-v) != grid1.getMissing();
				boolean data1ok = grid1.isValid(i,j) && grid1.getValue(i,j) != grid1.getMissing();
				if ( data0ok && data1ok ){
					int data0 = grid0.getValue(i-u,j-v);
					int data1 = grid1.getValue(i,j);
					int error = (data1-data0);
					tot_error += error*error;
					++num_data_valid;
				}
				if ( data1ok ){
					tot_data += grid1.getValue(i,j) * grid1.getValue(i,j);
				}
			}
		}
		if ( num_data_valid >= MIN_FILL_PIXELS && tot_data > 0 ){
			double error_ratio = tot_error / (double) tot_data;
			return error_ratio;
		}
		return 1; // greater than any valid error ratio
	}

	public static void test() throws Exception {
		// because the alignment doesn't really check lat-lon extents,
		// cropping from offset corners will look like translation ...
		LatLonGrid conus = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA);
		LatLonGrid[] grids = new LatLonGrid[2];
		grids[0] = conus.crop(900, 2500, 256, 256);
		grids[0].setMissing(0);
		int motx = -2; int moty = -3;
		grids[1] = conus.crop(900-motx, 2500-moty, 256, 256);
		grids[1].setMissing(0);
		CrossCorrelation alg = new CrossCorrelation(5,5,Math.abs(2*motx),Math.abs(2*moty));
		alg.compute(grids[0], grids[1], null);
		
		System.exit(0);
	}
	
	public static void main(String[] args) throws Exception {
		// test();
		// create output directory
		File out = OutputDirectory.getDefault("xcorr");
		
		// read
		File f = new File("data/seviri");
		Pair<LatLonGrid,Date>[] grids = SeviriInfraredTemperature.readAll(f);
		
		// do alg
		CrossCorrelation alg = new CrossCorrelation(3,3,5,5);
		Pair<LatLonGrid,LatLonGrid> motion = alg.compute(grids[0].first, grids[1].first, out);
		
		// write
		KmlWriter.write(motion.first, out, "xcorr_u", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(motion.second, out, "xcorr_v", PngWriter.createCoolToWarmColormap());
		
		// align and compute difference
		LatLonGrid diff = new AlignAndDifference().compute(grids[0].first, grids[1].first, motion, 1);
		KmlWriter.write(diff, out, "xcorr_diff", PngWriter.createCoolToWarmColormap());
	}

}
