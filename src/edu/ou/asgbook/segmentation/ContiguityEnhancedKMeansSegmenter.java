/**
 * 
 */
package edu.ou.asgbook.segmentation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Objects consist of pixels that are grown from initial centers using K-means
 * @author v.lakshmanan
 *
 */
public class ContiguityEnhancedKMeansSegmenter {
	// hardcoded for simplicity
	private final int MAX_ITER = 10;
	private final int NEIGH_HALF_SIZE = 1;
	private final float DISTANCE_WEIGHT = 0.1f; // alpha
	private final float DATAVALUE_WEIGHT = 0.8f; // beta
	private final float DISCONTIGUITY_WEIGHT = 0.1f; // gamma
	private final int START_THRESH;
	private final int MAX_DATA_DIFFERENCE;
	private final int MAX_CLUSTER_SIZE;
	private final int MIN_THRESH;
	
	/**
	 * KMeans is seeded from points > seed_value, so pass in a high enough value here
	 * Only pixels > min_thresh are eligible to be part of an object.
	 */
	public ContiguityEnhancedKMeansSegmenter(int min_thresh, int seed_value, int max_data_difference, int max_cluster_size) {
		this.START_THRESH = seed_value;
		this.MIN_THRESH = min_thresh;
		this.MAX_DATA_DIFFERENCE = max_data_difference;
		this.MAX_CLUSTER_SIZE = max_cluster_size;
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
			double data_dist = Math.abs(cval - value)/MAX_DATA_DIFFERENCE;
			
			// spatial distance from centroid
			double spat_dist_x = x - cx;
			double spat_dist_y = y - cy;
			double spat_dist = (spat_dist_x*spat_dist_x + spat_dist_y*spat_dist_y)/(MAX_CLUSTER_SIZE*MAX_CLUSTER_SIZE);
			
			return DISCONTIGUITY_WEIGHT * discontig_dist + DATAVALUE_WEIGHT * data_dist + DISTANCE_WEIGHT * spat_dist;
		}
	}
	
	private Cluster[] findClusters(LatLonGrid data, LabelResult label){
		Cluster[] clusters = new Cluster[label.maxlabel+1];
		clusters[0] = null; // make sure we never access the first one, which is background
		for (int i=1; i < clusters.length; ++i){
			clusters[i] = new Cluster();
		}
		if ( clusters.length == 0 ){
			return clusters;
		}
		int nrows = data.getNumLat();
		int ncols = data.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int regno = label.label.getValue(i, j);
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
	 * Creates a labeled grid where background pixels are set to 0
	 * and labels for objects go 1,2,3... All pixels > thresh are
	 * part of an object.
	 * Returns a LabelResult for each iteration, with the last one being
	 * the final result.
	 */
	public List<LabelResult> label(LatLonGrid data){
		List<LabelResult> result = new ArrayList<LabelResult>();
		final int nrows = data.getNumLat();
		final int ncols = data.getNumLon();
		
		// initialize based on simple thresholding at a high value
		final ThresholdSegmenter seeder = new ThresholdSegmenter(START_THRESH);
		LabelResult seed = seeder.label(data);
		result.add(seed); // first one
		
		// Start K-means
		int iter = 1;
		int n_changed = 0;
		do {
			// compute means
			Cluster[] clusters = findClusters(data, seed);
			// move pixels
			LabelResult next = new LabelResult(LatLonGrid.copyOf(seed.label), seed.maxlabel);
			n_changed = 0;
			for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
				if ( data.getValue(i,j) != data.getMissing() && data.getValue(i,j) > MIN_THRESH ){
					int closest = findClosestCluster(data.getValue(i,j),i,j,seed.label, clusters);
					if (closest != seed.label.getValue(i,j)){
						// change the label to closest
						next.label.setValue(i, j, closest);
						++n_changed;
					}
				}
			}
			System.out.println("Changing " + n_changed + " at " + iter + " th iteration");
			// for next step
			seed = next;
			result.add(seed);
			++iter;
		} while (iter < MAX_ITER && n_changed > 0);
		return result;
	}
	
	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("contigkmeans");
		
		LatLonGrid grid = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LinearScaling()).crop(1000, 2100, 100, 200);
		KmlWriter.write(grid, out, "orig", PngWriter.createCoolToWarmColormap());
		
		// label image based on threshold
		List<LabelResult> labels =  new ContiguityEnhancedKMeansSegmenter(10,20,100,10).label(grid);
		for (int i=0; i < labels.size(); ++i){
			LatLonGrid label = labels.get(i).label;
			KmlWriter.write(label, out, "label_" + i, PngWriter.createCoolToWarmColormap());
		}
	}
}
