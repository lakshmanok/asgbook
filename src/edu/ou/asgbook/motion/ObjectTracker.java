/**
 * 
 */
package edu.ou.asgbook.motion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Date;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pair;
import edu.ou.asgbook.dataset.SeviriInfraredTemperature;
import edu.ou.asgbook.filters.MedianFilter;
import edu.ou.asgbook.filters.SaturateFilter;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.segmentation.HysteresisSegmenter;
import edu.ou.asgbook.segmentation.LabelResult;
import edu.ou.asgbook.segmentation.RegionProperty;
import edu.ou.asgbook.segmentation.Segmenter;

/**
 * Estimates motion based on assigning objects in one frame to objects
 * in the previous frame.
 * 
 * @author v.lakshmanan
 *
 */
public class ObjectTracker implements MotionEstimator {
	public static class CentroidDistance implements CostEstimator {
		private int MAXDISTSQ = 100 * 100;
		@Override
		public int computeCost(RegionProperty a, RegionProperty b) {
			double distx = a.getCx() - b.getCx();
			double disty = a.getCy() - b.getCy();
			int distsq = (int) Math.round( distx*distx + disty*disty );
			if (distsq > MAXDISTSQ){
				return MAXDISTSQ;
			}
			return distsq;
		}
		@Override
		public int getMaxCost() {
			return MAXDISTSQ;
		}
	}
	private static class GreedyAssigment implements Assigner {
		public int[] getAssignments(int[][] cost, int maxcost){
			// just do it greedily for now.
			int[] assigned = new int[cost.length];
			for (int i=1; i < assigned.length; ++i){
				int bestj = 0;
				int bestcost = maxcost;
				for (int j=1; j < cost[i].length; ++j){
					if (cost[i][j] < bestcost){
						// repeat assignments okay
						bestcost = cost[i][j];
						bestj = j;
					}
				}
				assigned[i] = bestj;
			}
			return assigned;
		}
	}
	
	public static class SimpleSegmenter implements Segmenter {
		private final Segmenter segmenter;
		private final int MIN_SIZE;
		
		public SimpleSegmenter(int hysThresh1, int hysThresh2, int minsize){
			segmenter = new HysteresisSegmenter(hysThresh1, hysThresh2);
			MIN_SIZE = minsize;
		}
		
		@Override
		public LabelResult label(LatLonGrid data) {
			LabelResult all = segmenter.label(data);
			LabelResult pruned = RegionProperty.pruneBySize(all, data, MIN_SIZE);
			System.out.println("After pruning, maxlabel=" + pruned.maxlabel + " from " + all.maxlabel);
			return pruned;
		}		
	}
	
	private final Segmenter segmenter;
	private final CostEstimator costEstimator;
	private final Assigner assigner;
	private final int MOT_SCALE = 10;
	
	public interface CostEstimator {
		int computeCost(RegionProperty a, RegionProperty b);
		int getMaxCost();
	}
	
	public interface Assigner {
		int[] getAssignments(int[][] cost, int maxcost);
	}
	
	public ObjectTracker(int hysThresh1, int hysThresh2, int minsize){
		this(new SimpleSegmenter(hysThresh1, hysThresh2, minsize),
				new CentroidDistance(), new GreedyAssigment());
	}
	
	public ObjectTracker(Segmenter seg, CostEstimator cost, Assigner a){
		segmenter = seg;
		costEstimator = cost;
		assigner = a;
	}
	
	private int[][] computeCost(RegionProperty[] frame0, RegionProperty[] frame1){
		int[][] cost = new int[frame1.length][frame0.length];
		for (int i=0; i < cost.length; ++i) for (int j=0; j < cost[i].length; ++j){
			RegionProperty a = frame0[j];
			RegionProperty b = frame1[i];
			if (a != null && b != null){
				cost[i][j] = costEstimator.computeCost(a,b);
			} else {
				cost[i][j] = costEstimator.getMaxCost();
			}
		}
		return cost;
	}
	
	private int[] getAssignments(int[][] cost, File outdir){
		// use the assigner
		int[] assigned = assigner.getAssignments(cost, costEstimator.getMaxCost());
		
		if (outdir != null){
			PrintWriter out = null;
			try {
				out = new PrintWriter(new File(outdir.getAbsolutePath() + "/assignment.txt"));
				for (int i=1; i < cost.length; ++i){
					for (int j=1; j < cost[i].length; ++j){
						out.print(cost[i][j]);
						if (assigned[i] == j){
							out.print("* ");
						} else {
							out.print(" ");
						}
						if (j != (cost[i].length-1)){
							out.print(" & ");
						}
					}
					out.println("\\\\");
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				if (out != null){
					out.close();
				}
			}
		}
		
		return assigned;
	}
	
	@Override
	public Pair<LatLonGrid, LatLonGrid> compute(LatLonGrid data0, LatLonGrid data1, File outdir){
		LabelResult objects0 = segmenter.label(data0);
		LabelResult objects1 = segmenter.label(data1);
		
		if (outdir != null){
			try {
				KmlWriter.write(objects0.label, outdir, "objects0", PngWriter.createRandomColormap());
				KmlWriter.write(objects1.label, outdir, "objects1", PngWriter.createRandomColormap());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// match the objects across frames
		RegionProperty[] regions0 = RegionProperty.compute(objects0, data0);
		RegionProperty[] regions1 = RegionProperty.compute(objects1, data1);
		int[][] cost = computeCost(regions0, regions1);
		int[] assigned = getAssignments(cost, outdir);
		
		// find motion for each region
		int[] regu = new int[assigned.length];
		int[] regv = new int[assigned.length];
		for (int i=1; i < assigned.length; ++i){
			int oldregno = assigned[i];
			if ( oldregno > 0 ){
				double cx = regions1[i].getCx();
				double cy = regions1[i].getCy();
				double oldcx = regions0[oldregno].getCx();
				double oldcy = regions0[oldregno].getCy();
				regu[i] = (int) Math.round( (cx - oldcx)*MOT_SCALE );
				regv[i] = (int) Math.round( (cy - oldcy)*MOT_SCALE );
				// System.out.println("Object at " + cx + "," + cy + " moving at " + regu[i] + "," + regv[i]);
			}
		}
		
		// apply the motion estimate based on assignment to all pixels
		LatLonGrid u = new LatLonGrid(data0.getNumLat(), data0.getNumLon(), 0, data0.getNwCorner(), data0.getLatRes(), data0.getLonRes());
		LatLonGrid v = LatLonGrid.copyOf(u);
		for (int i=0; i < u.getNumLat(); ++i) for (int j=0; j < u.getNumLon(); ++j){
			int regno = objects1.label.getValue(i,j);
			if ( regno > 0 ){
				u.setValue(i,j, regu[regno]);
				v.setValue(i,j, regv[regno]);
			}
		}
		
		return new Pair<LatLonGrid,LatLonGrid>(u,v);
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("objecttracker");
		
		// read
		File f = new File("data/seviri");
		Pair<LatLonGrid,Date>[] grids = SeviriInfraredTemperature.readAll(f);
		
		// do alg
		MotionEstimator alg = new ObjectTracker(100, 110, 1000);
		MedianFilter smoother = new MedianFilter(10);
		LatLonGrid grid0 = smoother.filter(grids[0].first);
		LatLonGrid grid1 = smoother.filter(grids[1].first);
		Pair<LatLonGrid,LatLonGrid> motion = alg.compute(grid0, grid1, out);
		
		// write
		SaturateFilter filter = new SaturateFilter(-150, 150);
		LatLonGrid u = filter.filter(motion.first);
		LatLonGrid v = filter.filter(motion.second);
		KmlWriter.write(u, out, "closest_u", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(v, out, "closest_v", PngWriter.createCoolToWarmColormap());
	}
}
