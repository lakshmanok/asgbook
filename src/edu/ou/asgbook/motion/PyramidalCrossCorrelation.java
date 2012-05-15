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
 * Cross-correlation at muliple resolutions.
 * 
 * @author v.lakshmanan
 *
 */
public class PyramidalCrossCorrelation implements MotionEstimator {
	private final int MAX_U;
	private final int MAX_V;
	
	/**
	 * Pass in the maximum movement in the two directions.
	 */
	public PyramidalCrossCorrelation(int maxmotion_x, int maxmotion_y) {
		MAX_U = maxmotion_x;
		MAX_V = maxmotion_y;
	}

	private int nextMultiple(int x, int factor){
		if ( x%factor == 0 ){
			return x;
		}
		return factor * (x/factor + 1);
	}
	
	@Override
	public Pair<LatLonGrid, LatLonGrid> compute(LatLonGrid data0, LatLonGrid data1, File outdir){
		int numres = (int) Math.round( Math.log( Math.max(MAX_U,MAX_V))/Math.log(2) );
		System.out.println("Will do computation at " + numres + " resolutions");
		
		// add missing data to the borders to get them to be divisible by factor
		int factor = (int) ( Math.pow(2, numres) + 0.01 );
		data0 = pad( data0, nextMultiple(data0.getNumLat(),factor), nextMultiple(data0.getNumLon(),factor) );
		data1 = pad( data1, nextMultiple(data0.getNumLat(),factor), nextMultiple(data0.getNumLon(),factor) );
		
		LatLonGrid aligned_data0 = data0;
		LatLonGrid u = null;
		LatLonGrid v = null;
		for (int res = numres; res >= 0; --res){
			// shift data0 using u,v
			if (u != null){
				aligned_data0 = align(data0, u, v);
				if (outdir != null){
					try {
						KmlWriter.write(aligned_data0, outdir, "pxaligned_" + res, PngWriter.createCoolToWarmColormap());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			// downsample grids to this resolution
			int smsize = (int) ( Math.pow(2, res) + 0.01 ); // at res=4, this is 2^4 or 16
			LatLonGrid grid0 = decreaseSize(aligned_data0, smsize);
			LatLonGrid grid1 = decreaseSize(data1, smsize);
			if (outdir != null){
				try {
					KmlWriter.write(grid0, outdir, "pxdata0_" + res, PngWriter.createCoolToWarmColormap());
					KmlWriter.write(grid1, outdir, "pxdata1_" + res, PngWriter.createCoolToWarmColormap());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// find u,v at this resolution
			int est_size = numres-res; // 0 at coarsest resolution, +1 with each resolution
			int search_radius = 1;
			CrossCorrelation xcorr = new CrossCorrelation(est_size, est_size, search_radius, search_radius);
			Pair<LatLonGrid,LatLonGrid> motion = xcorr.compute(grid0, grid1, outdir);
			LatLonGrid thisu = increaseSize(motion.first, smsize);
			LatLonGrid thisv = increaseSize(motion.second, smsize);
			thisu = multiply(thisu, smsize); // movement of 1 pixel at res=4 is equal to movement of 16 in original image
			thisv = multiply(thisv, smsize);
			// update the total u,v
			u = (u != null)? LatLonGrid.add(thisu, u) : thisu;
			v = (v != null)? LatLonGrid.add(thisv, v) : thisv;
			if (outdir != null){
				try {
					KmlWriter.write(u, outdir, "pxu_" + res, PngWriter.createCoolToWarmColormap());
					KmlWriter.write(u, outdir, "pxv_" + res, PngWriter.createCoolToWarmColormap());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return new Pair<LatLonGrid,LatLonGrid>(u,v);
	}

	private LatLonGrid multiply(LatLonGrid in, int bythis) {
		LatLonGrid out = LatLonGrid.copyOf(in);
		for (int i=0; i < in.getNumLat(); ++i){
			for (int j=0; j < in.getNumLon(); ++j){
				out.setValue(i,j, in.getValue(i,j) * bythis);
			}
		}
		return out;
	}

	private LatLonGrid pad(LatLonGrid data0, int nrows, int ncols) {
		System.out.println("Padding image from " + data0.getNumLat() + "x" + data0.getNumLon() + " to " + nrows + "x" + ncols);
		LatLonGrid out = new LatLonGrid(nrows, ncols, data0.getMissing(), data0.getNwCorner(), data0.getLatRes(), data0.getLonRes());
		out.fill(out.getMissing());
		for (int i=0; i < data0.getNumLat(); ++i){
			for (int j=0; j < data0.getNumLon(); ++j){
				out.setValue(i,j, data0.getValue(i,j));
			}
		}
		return out;
	}

	private LatLonGrid align(LatLonGrid in, LatLonGrid u, LatLonGrid v) {
		LatLonGrid out = LatLonGrid.copyOf(in);
		out.fill(out.getMissing());
		// do a "get"
		for (int i=0; i < u.getNumLat(); ++i) for (int j=0; j < u.getNumLon(); ++j){
			int fromx = i - u.getValue(i,j);
			int fromy = j - v.getValue(i,j);
			if (in.isValid(fromx,fromy)){
				int value = in.getValue(fromx, fromy);
				out.setValue(i,j, value);
			}
		}
		return out;
	}

	private LatLonGrid decreaseSize(LatLonGrid in, int factor) {
		int smsize = factor;
		if (smsize%2 == 0) ++smsize;
		ConvolutionFilter smFilter = new ConvolutionFilter(ConvolutionFilter.boxcar(smsize, smsize));
		in = smFilter.smooth(in);
		LatLonGrid out = new LatLonGrid(in.getNumLat()/factor, in.getNumLon()/factor, in.getMissing(), in.getNwCorner(), in.getLatRes()*factor, in.getLonRes()*factor );
		for (int i=0; i < out.getNumLat(); ++i){
			for (int j=0; j < out.getNumLon(); ++j){
				int x = i*factor;
				int y = j*factor;
				out.setValue(i, j, in.getValue(x,y));
			}
		}
		return out;
	}
	
	private LatLonGrid increaseSize(LatLonGrid in, int factor) {
		LatLonGrid out = new LatLonGrid(in.getNumLat()*factor, in.getNumLon()*factor, in.getMissing(), in.getNwCorner(), in.getLatRes()/factor, in.getLonRes()/factor );
		for (int i=0; i < out.getNumLat(); ++i){
			for (int j=0; j < out.getNumLon(); ++j){
				int x = i/factor;
				int y = j/factor;
				out.setValue(i, j, in.getValue(x,y));
			}
		}
		return out;
	}

	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("pyramidxcorr");
		
		// read
		File f = new File("data/seviri");
		Pair<LatLonGrid,Date>[] grids = SeviriInfraredTemperature.readAll(f);
		
		// do alg
		PyramidalCrossCorrelation alg = new PyramidalCrossCorrelation(20,20);
		Pair<LatLonGrid,LatLonGrid> motion = alg.compute(grids[0].first, grids[1].first, out);
		
		// write
		KmlWriter.write(motion.first, out, "pxfinal_u", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(motion.second, out, "pxfinal_v", PngWriter.createCoolToWarmColormap());
	}
}
