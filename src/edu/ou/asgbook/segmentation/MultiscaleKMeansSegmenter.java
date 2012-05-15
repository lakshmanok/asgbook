/**
 * 
 */
package edu.ou.asgbook.segmentation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.SurfaceAlbedo;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Quantizes image into K levels, then does multiscale segmentation
 * Does not implement the pruning techniques discussed in the paper.
 * 
 * See: V. Lakshmanan, R. Rabin, and V. DeBrunner, ``Multiscale storm identification and forecast,'' J. Atm. Res., vol. 67, pp. 367-380, July 2003
 * @author v.lakshmanan
 *
 */
public class MultiscaleKMeansSegmenter {
	// hardcoded for simplicity
	private final int MAX_ITER = 10;
	private final int NEIGH_HALF_SIZE = 1;
	private final float DISCONTIGUITY_WEIGHT = 0.8f;
	private final float DATAVALUE_WEIGHT = 0.2f;

	private final int THRESH1, THRESH2, K;
	private final double INCR;
	
	/**
	 * Specify contouring levels
	 */
	public MultiscaleKMeansSegmenter(int thresh1, int thresh2, int K) {
		this.THRESH1 = thresh1;
		this.THRESH2 = thresh2;
		this.K = K;
		this.INCR = (double)(THRESH2 - THRESH1) / K;
	}

	private class Cluster {
		double cx, cy, cval;
		double totwt;
		int N;
		Cluster() { totwt = cx = cy = cval = 0; N = 0; }
		public double computeDistance(final int value, final int x, final int y, final LatLonGrid label) {
			// discontiguity
			int n_different = 0;
			int n_neigh = 0;
			int centerlabel = label.getValue(x,y);
			for (int i=x-NEIGH_HALF_SIZE; i <= (x+NEIGH_HALF_SIZE); ++i){
				for (int j=y-NEIGH_HALF_SIZE; j <= (y+NEIGH_HALF_SIZE); ++j){
					if ( label.isValid(i, j) ){
						if( label.getValue(i, j) != centerlabel ){
							++n_different;
						}
						++n_neigh;
					}
				}
			}
			double discontig_dist = (n_neigh < 2)? n_different : ((double)n_different)/n_neigh;
			
			// distance based on data value
			double data_dist = Math.abs(cval - value)/INCR;
						
			return DISCONTIGUITY_WEIGHT * discontig_dist + DATAVALUE_WEIGHT * data_dist;
		}
	}
	
	private Cluster[] findClusters(LatLonGrid data, LatLonGrid label, int maxlabel){
		Cluster[] clusters = new Cluster[maxlabel+1];
		clusters[0] = null; // make sure we never access the first one, which is background
		for (int i=0; i < clusters.length; ++i){
			clusters[i] = new Cluster();
		}
		if ( clusters.length == 0 ){
			return clusters;
		}
		int nrows = data.getNumLat();
		int ncols = data.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int regno = label.getValue(i, j);
			if (regno != 0){
				int wt = data.getValue(i,j);
				clusters[regno].cx += wt*i;
				clusters[regno].cy += wt*j;
				clusters[regno].cval += wt;
				clusters[regno].N ++;
				clusters[regno].totwt += wt;
			}
		}
		for (int i=1; i < clusters.length; ++i){
			if ( clusters[i].N > 0 ){
				clusters[i].cx /= clusters[i].totwt;
				clusters[i].cy /= clusters[i].totwt;
				clusters[i].cval /= clusters[i].N;
				//System.out.println(clusters[i].cx + "," + clusters[i].cy + ": " + clusters[i].N + " of " + clusters[i].cval);
			}
		}
		return clusters;
	}
	
	private int findClosestCluster(int value, int x, int y, LatLonGrid label, Cluster[] clusters) {
		int best = 0;
		double mindist = 1; // no distances > 1 considered
		for (int c = 1; c < clusters.length; ++c){ // 0 is background
			double dist = clusters[c].computeDistance(value,x,y,label);
			if (dist < mindist){
				mindist = dist;
				best = c;
			}
		}
		return best;
	}

	
	/**
	 * Contours grid into levels 1,2,3...K
	 */
	public LatLonGrid quantize(LatLonGrid data, File out){
		final int nrows = data.getNumLat();
		final int ncols = data.getNumLon();
		
		// initialize based on simple quantization
		LatLonGrid seed = LatLonGrid.copyOf(data);
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int levelno = 0;
			if (data.getValue(i, j) != data.getMissing()){
				levelno = (int) Math.round((data.getValue(i,j) - THRESH1)/INCR);
				if ( levelno < 0 ) levelno = 0;
				else if ( levelno > K ) levelno = K;
			}
			seed.setValue(i,j, levelno);
		}
		if (out != null){
			try {
				KmlWriter.write(seed, out, "levels_0", PngWriter.createCoolToWarmColormap());
			} catch (Exception e) {
				System.err.println(e);
			}
		}
		
		// Start K-means
		int iter = 1;
		int n_changed = 0;
		do {
			// compute means: could get away with simply using center of data range ...
			Cluster[] clusters = findClusters(data, seed, K);
			// move pixels
			LatLonGrid next = LatLonGrid.copyOf(seed);
			n_changed = 0;
			for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
				if ( data.getValue(i,j) != data.getMissing() ){
					int closest = findClosestCluster(data.getValue(i,j),i,j,seed, clusters);
					if (closest != seed.getValue(i,j)){
						// change the label to closest
						next.setValue(i, j, closest);
						++n_changed;
					}
				}
			}
			System.out.println("Changing " + n_changed + " at " + iter + " th iteration");
			// for next step
			seed = next;
			
			if (out != null){
				try {
					KmlWriter.write(seed, out, "levels_" + iter, PngWriter.createCoolToWarmColormap());
				} catch (Exception e) {
					System.err.println(e);
				}
			}
			
			++iter;
		} while (iter < MAX_ITER && n_changed > 0);
		return seed;
	}
	
	/**
	 * Returns K scales of output
	 */
	public List<LabelResult> label(LatLonGrid data, File out){
		List<LabelResult> result = new ArrayList<LabelResult>();
		LatLonGrid levels = quantize(data, out);
		for (int thresh=1; thresh <= K; ++thresh){
			ThresholdSegmenter seg = new ThresholdSegmenter(thresh);
			LabelResult label = seg.label(levels);
			if (out != null){
				try {
					KmlWriter.write(label.label, out, "label_" + thresh, PngWriter.createCoolToWarmColormap());
				} catch (Exception e) {
					System.err.println(e);
				}
			}
			result.add(label);
		}
		return result;
	}
	
	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("multiscalekmeans");
		
		// data
		LatLonGrid conus = SurfaceAlbedo.read(SurfaceAlbedo.CONUS, 100).crop(100, 100, 500, 200);
		KmlWriter.write(conus, out, "orig", PngWriter.createCoolToWarmColormap());
		
		new MultiscaleKMeansSegmenter(20,25,5).label(conus, out);
	}
}
