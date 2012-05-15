/**
 * 
 */
package edu.ou.asgbook.motion;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pair;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.dataset.SeviriInfraredTemperature;
import edu.ou.asgbook.filters.MedianFilter;
import edu.ou.asgbook.filters.SaturateFilter;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.segmentation.LabelResult;
import edu.ou.asgbook.segmentation.RegionProperty;
import edu.ou.asgbook.segmentation.Segmenter;

/**
 * 
 * Estimates motion by finding cross-correlation of objects in one frame
 * to the pixels in the previous frame.
 * 
 * @author v.lakshmanan
 *
 */
public class HybridTracker implements MotionEstimator {
	private final int MAX_U;
	private final int MAX_V;
	private final Segmenter segmenter;
	
	public HybridTracker(Segmenter seg, int maxmotionx, int maxmotiony){
		MAX_U = maxmotionx;
		MAX_V = maxmotiony;
		segmenter = seg;
	}
	
	private static class Centroid {
		double cx;
		double cy;
		int motx;
		int moty;
		int size;
	}
	
	@Override
	public Pair<LatLonGrid, LatLonGrid> compute(LatLonGrid data0, LatLonGrid data1, File outdir){
		LabelResult objects1 = segmenter.label(data1);
		if (outdir != null){
			try {
				KmlWriter.write(objects1.label, outdir, "hybobjects1", PngWriter.createRandomColormap());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// find motion for each region and apply it to all pixels for that region
		Pixel[][] regions = RegionProperty.getPixelsInRegions(data1, objects1);
		LatLonGrid u = new LatLonGrid(data0.getNumLat(), data0.getNumLon(), 0, data0.getNwCorner(), data0.getLatRes(), data0.getLonRes());
		LatLonGrid v = LatLonGrid.copyOf(u);
		RegionProperty[] regprop = RegionProperty.compute(objects1, data1);
		List<Centroid> centroids = new ArrayList<Centroid>();
		for (int reg=1; reg < regions.length; ++reg){
			Pair<Integer,Integer> motion = computeMotion(regions[reg], data0);
			int motx = motion.first;
			int moty = motion.second;
			Centroid c = new Centroid();
			c.cx = regprop[reg].getCx();
			c.cy = regprop[reg].getCy();
			c.motx = motx;
			c.moty = moty;
			c.size = regprop[reg].getSize();
			centroids.add(c);
			for (Pixel p : regions[reg]){
				u.setValue(p.getX(), p.getY(), motx);
				v.setValue(p.getX(), p.getY(), moty);
			}
		}
		
		if (outdir != null){
			try {
				KmlWriter.write(u, outdir, "u_beforeinterp", PngWriter.createCoolToWarmColormap());
				KmlWriter.write(v, outdir, "v_beforeinterp", PngWriter.createCoolToWarmColormap());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// interpolate inbetween regions if you have enough of them ...
		if ( centroids.size() > 1 ){
			LatLonGrid interpu = LatLonGrid.copyOf(u);
			LatLonGrid interpv = LatLonGrid.copyOf(v);
			for (int i=0; i < interpu.getNumLat(); ++i) for (int j=0; j < interpu.getNumLon(); ++j){
				double totu = 0;
				double totv = 0;
				double totwt = 0;
				for (Centroid c : centroids){
					double distx = c.cx - i;
					double disty = c.cy - j;
					double distsq = distx*distx + disty*disty;
					double wt = c.size * 1.0/(distsq*distsq + 0.0001); // 1/r^2
					totu += c.motx * wt;
					totv += c.moty * wt;
					totwt += wt;
				}
				interpu.setValue(i, j, (int) Math.round(totu/totwt));
				interpv.setValue(i, j, (int) Math.round(totv/totwt));
			}
		
			if (outdir != null){
				try {
					KmlWriter.write(interpu, outdir, "u_interp", PngWriter.createCoolToWarmColormap());
					KmlWriter.write(interpv, outdir, "v_interp", PngWriter.createCoolToWarmColormap());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			return new Pair<LatLonGrid,LatLonGrid>(interpu,interpv);
		}
		
		return new Pair<LatLonGrid,LatLonGrid>(u,v);
	}
	
	private Pair<Integer, Integer> computeMotion(Pixel[] region, LatLonGrid data) {
		int bestm = 0;
		int bestn = 0;
		int besterror = Integer.MAX_VALUE;
		for (int m=-MAX_U; m <= MAX_U; ++m) for (int n=-MAX_V; n <= MAX_V; ++n){
			int tot_error = 0;
			for (Pixel p : region){
				int oldx = p.getX() - m;
				int oldy = p.getY() - n;
				if (data.isValid(oldx, oldy)){
					int error = Math.abs(p.getValue() - data.getValue(oldx, oldy)); // abs error
					tot_error += error;
				}
			}
			if ( tot_error < besterror ){
				besterror = tot_error;
				bestm = m;
				bestn = n;
			}
		}
		return new Pair<Integer,Integer>(bestm,bestn);
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("hybridtracker");
		
		// read
		File f = new File("data/seviri");
		Pair<LatLonGrid,Date>[] grids = SeviriInfraredTemperature.readAll(f);
		
		// do alg
		Segmenter seg = new ObjectTracker.SimpleSegmenter(100, 110, 100);
		MotionEstimator alg = new HybridTracker( seg, 20, 20 );
		MedianFilter smoother = new MedianFilter(10);
		LatLonGrid grid0 = smoother.filter(grids[0].first);
		LatLonGrid grid1 = smoother.filter(grids[1].first);
		Pair<LatLonGrid,LatLonGrid> motion = alg.compute(grid0, grid1, out);
		
		LatLonGrid diff = new AlignAndDifference().compute(grids[0].first, grids[1].first, motion);
		KmlWriter.write(diff, out, "hybriddiff", PngWriter.createCoolToWarmColormap());
		
		// write
		SaturateFilter filter = new SaturateFilter(-15, 15);
		LatLonGrid u = filter.filter(motion.first);
		LatLonGrid v = filter.filter(motion.second);
		KmlWriter.write(u, out, "opticflow_u", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(v, out, "opticflow_v", PngWriter.createCoolToWarmColormap());
	}
}
