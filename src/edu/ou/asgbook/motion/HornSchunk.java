/**
 * 
 */
package edu.ou.asgbook.motion;

import java.io.File;
import java.util.Date;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pair;
import edu.ou.asgbook.dataset.SeviriInfraredTemperature;
import edu.ou.asgbook.filters.ConvolutionFilter;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Horn-Schunk optical flow method of motion estimation
 * 
 * @author v.lakshmanan
 *
 */
public class HornSchunk implements MotionEstimator {
	private final int SM_HALFSIZE_NS = 5; // smoothing window size
	private final int SM_HALFSIZE_EW = 5; // smoothing window size
	private final double ALPHASQ = 100; // smoothness factor
	private final int MAX_ITER = 50;
	public static final int MOT_SCALE = 10; // motion estimates are multiplied by 10 to make them integers


	@Override
	public Pair<LatLonGrid, LatLonGrid> compute(LatLonGrid data0, LatLonGrid data1, File outdir){
		// Grids we need. initialize all of them at zero
		final int nrows = data1.getNumLat();
		final int ncols = data1.getNumLon();
		LatLonGrid I_x = new LatLonGrid(nrows, ncols, 0, data1.getNwCorner(), data1.getLatRes(), data1.getLonRes());
		LatLonGrid I_y = LatLonGrid.copyOf(I_x);
		LatLonGrid I_t = LatLonGrid.copyOf(I_x);
		LatLonGrid u = LatLonGrid.copyOf(I_x);
		LatLonGrid v = LatLonGrid.copyOf(I_x);
		
		// compute gradient of intensity in x, y and t directions
		for (int i=1; i < nrows-1; ++i) for (int j=1; j < ncols-1; ++j){
			int i_t = data1.getValue(i,j) - data0.getValue(i,j);   // time
			int i_x = data1.getValue(i,j) - data1.getValue(i-1,j); // lat
			int i_y = data1.getValue(i,j) - data1.getValue(i,j-1); // lon
			I_x.setValue(i,j, i_x);
			I_y.setValue(i,j, i_y);
			I_t.setValue(i,j, i_t);
		}

		// write intermediates
		if (outdir != null){
			try {
				KmlWriter.write(I_x, outdir, "I_x", PngWriter.createCoolToWarmColormap());
				KmlWriter.write(I_y, outdir, "I_y", PngWriter.createCoolToWarmColormap());
				KmlWriter.write(I_t, outdir, "I_t", PngWriter.createCoolToWarmColormap());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// now iterate
		for (int iter=0; iter < MAX_ITER; ++iter){
			// compute meanu, meanv
			LatLonGrid meanu, meanv;
			if ( iter == 0 ){
				meanu = LatLonGrid.copyOf(u);
				meanv = LatLonGrid.copyOf(v);
			} else {
				ConvolutionFilter boxcar = new ConvolutionFilter(ConvolutionFilter.boxcar(2*SM_HALFSIZE_NS+1, 2*SM_HALFSIZE_EW+1));
				meanu = boxcar.smooth(u);
				meanv = boxcar.smooth(v);
			}
			
			for (int i=1; i < nrows-1; ++i) for (int j=1; j < ncols-1; ++j){
				double u_k = meanu.getValue(i, j)/(double)MOT_SCALE;
				double v_k = meanv.getValue(i, j)/(double)MOT_SCALE;
				int i_x = I_x.getValue(i,j);
				int i_y = I_y.getValue(i,j);
				int i_t = I_t.getValue(i,j);
				double corr = (i_x*u_k + i_y*v_k + i_t) / (ALPHASQ + i_x*i_x + i_y*i_y);
				u.setValue(i,j, (int) Math.round((u_k - i_x*corr)*MOT_SCALE));
				v.setValue(i,j, (int) Math.round((v_k - i_y*corr)*MOT_SCALE));
			}
			
			if (outdir != null && iter == 0 || iter == 1 || iter == MAX_ITER/2){
				try {
					KmlWriter.write(u, outdir, "motionNS_"+iter, PngWriter.createCoolToWarmColormap());
					KmlWriter.write(v, outdir, "motionEW_"+iter, PngWriter.createCoolToWarmColormap());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return new Pair<LatLonGrid,LatLonGrid>(u,v);
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("hornschunk");
		
		// read
		File f = new File("data/seviri");
		Pair<LatLonGrid,Date>[] grids = SeviriInfraredTemperature.readAll(f);
		
		// do alg
		MotionEstimator alg = new HornSchunk();
		Pair<LatLonGrid,LatLonGrid> motion = alg.compute(grids[0].first, grids[1].first, out);
		
		// write
		KmlWriter.write(motion.first, out, "opticflow_u", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(motion.second, out, "opticflow_v", PngWriter.createCoolToWarmColormap());
		
		// align and compute difference
		LatLonGrid diff = new AlignAndDifference().compute(grids[0].first, grids[1].first, motion, MOT_SCALE);
		KmlWriter.write(diff, out, "opticflow_diff", PngWriter.createCoolToWarmColormap());
	}

}
