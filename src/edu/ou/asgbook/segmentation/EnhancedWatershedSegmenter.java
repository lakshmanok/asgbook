/**
 * 
 */
package edu.ou.asgbook.segmentation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.filters.ConvolutionFilter;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Enhanced watershed segmentation following Lakshmanan, Hondl and Rabin
 * @author valliappa.lakshmanan
 *
 */
public class EnhancedWatershedSegmenter implements Segmenter {
	private final int myDelta;
	private final int myMinSize;
	private final int minThresh;
	private final int dataIncr;
	private final int maxThresh;
	private static final int UNMARKED = -1;
	private static final int GLOBBED = -3; // not UNMARKED
	private static final int TOOSMALL = -4;

	/**
	 * @param minThresh: minimum pixel value for a pixel to be part of a region
	 * @param dataIncr:  quantization interval. Use 1 if you don't want to quantize
	 * @param maxThresh: values > maxThresh are treated as maxThresh
		@param  sizeThresholdInPixels: Blobs smaller than the specified size will get ignored.
		@param deltaForCluster: Specify how many data-increments a cluster is allowed
		to range over. For example, if you specify 0, then a cluster will contain
		only values that fall in the same interval as the maximum. 
		Larger values of D also yield clusters at larger scales.
	 */
	public EnhancedWatershedSegmenter(int minThresh, int dataIncr, int maxThresh, int sizeThresholdInPixels, int deltaForCluster) {
		this.myDelta = deltaForCluster;
		this.myMinSize = sizeThresholdInPixels;
		this.minThresh = minThresh;
		this.maxThresh = maxThresh;
		this.dataIncr = dataIncr;
	}  

	@SuppressWarnings("serial")
	private static class Pixels extends ArrayList<Pixel> {
	}
	private static class Glob {
		Pixel center;
		Pixels asglob;
	}

	private boolean isClosest(Pixel p, Pixel center, List<Pixel>[] otherCenters, LatLonGrid data){
		final int mybin  = center.getValue();
		final int binthresh = mybin / 2;  // half the height
		final int mydist = p.getDistanceSquared(center);
		for (int obin = binthresh; obin < otherCenters.length; ++obin)
			for (int i=0; i < otherCenters[obin].size(); ++i)
				if (p.getDistanceSquared(otherCenters[obin].get(i)) < mydist)
					return false;
		return true;
	}

	/** @return whether this maximum has been captured */
	private boolean setMaximum( LatLonGrid data, LatLonGrid marked,
			Pixel center, int bin_lower , int minSize,
			List<Pixel>[] centers, List<Glob> toglob ){
		// asbin -> pixels to be included in peak
		// asglob -> pixels to be globbed up as part of foothills
		// markedsofar -> pixels that have already been marked
		Pixels asbin = new Pixels();
		Pixels asglob = new Pixels();
		Pixels markedsofar = new Pixels();
		boolean willBeConsideredAgain = false;
		asbin.add( center );
		while ( asbin.size() != 0 ){
			// mark this point
			Pixel p = asbin.remove(asbin.size()-1);
			int x = p.getX();
			int y = p.getY();
			if ( marked.getValue(x,y) != UNMARKED ) continue; // already processed

			marked.setValue(x,y, center.getValue());
			markedsofar.add(p);

			// check neighbors
			for (int i=x-1; i <= (x+1); ++i)
				for (int j=y-1; j <= (y+1); ++j){
					boolean ok = data.isValid(i,j);
					if (!ok) continue;

					// is neighbor part of peak or part of mountain?
					if ( marked.getValue(i,j) == UNMARKED ){
						Pixel pixel = new Pixel(i,j, data.getValue(i,j));
						if (!willBeConsideredAgain && data.getValue(i,j) >= 0 && data.getValue(i,j) < center.getValue() )
							willBeConsideredAgain = true;

						if ( data.getValue(i,j) >= bin_lower )
							asbin.add( pixel );
						// Do not check that this is the closest: this way, a narrow channel of globbed pixels forms around this region and prevents it being mixed up with its foothills
						else if ( data.getValue(i,j) >= 0 ) // && isClosest(pixel,center,centers,data) )
							asglob.add( pixel );
					}
				}
		}

		if (center.getValue() == 0)
			willBeConsideredAgain = false;
		boolean bigEnough = (markedsofar.size() >= minSize);

		if ( bigEnough ){
			// remove lower values within region of influence
			Glob glob = new Glob();
			glob.center = center;
			Pixels temp = asglob; asglob = glob.asglob; glob.asglob = temp; // swap
			toglob.add(glob);
		} else if (willBeConsideredAgain){  // remove the check if you don't want small regions colored
			for (int i=0; i < markedsofar.size(); ++i){
				Pixel p = markedsofar.get(i);
				marked.setValue(p.getX(), p.getY(), UNMARKED);
			}
			asbin.clear(); asglob.clear(); markedsofar.clear();
		}

		return (bigEnough || !willBeConsideredAgain);
	}

	private void removeFoothills( LatLonGrid data, LatLonGrid marked, int bin, int bin_lower,
			List<Pixel>[] centers, List<Glob> toglob ){
		// anything marked too small, set it back to UNMARKED, so that
		// a lower pixel will take care of it
		marked.replace( TOOSMALL, UNMARKED );
		for (int g = 0; g < toglob.size(); ++g){
			Pixel center = toglob.get(g).center;
			Pixels asglob = toglob.get(g).asglob;
			// remove all foothills
			while ( asglob.size() != 0 ){
				// mark this point
				Pixel pt = asglob.remove(asglob.size()-1);
				int x = pt.getX();
				int y = pt.getY();
				marked.setValue(x,y, GLOBBED);
				for (int i=x-1; i <= (x+1); ++i)
					for (int j=y-1; j <= (y+1); ++j){
						if (!data.isValid(i,j)) continue;
						// is neighbor part of peak or part of mountain?
						if ( marked.getValue(i,j) == UNMARKED ){
							Pixel pn = new Pixel(i,j, data.getValue(i,j));
							// will let in even minor peaks
							if ( data.getValue(i,j) >= 0 && data.getValue(i,j) < bin_lower &&
									(data.getValue(i,j) <= data.getValue(x,y) || isClosest(pn, center, centers, data)) )
								asglob.add(pn);
						}
					}
			}// while loop
		}//for loop 
		toglob.clear();
	}

	private LatLonGrid findLocalMaxima(LatLonGrid dataval){
		// quantize: set data to the bin number
		final int maxbin = (maxThresh - minThresh)/dataIncr;
		Pixels[] pixels = new Pixels[ maxbin+1 ];
		for (int i=0; i < pixels.length; ++i){
			pixels[i] = new Pixels();
		}
		LatLonGrid data = LatLonGrid.copyOf(dataval);
		data.fill(-1);
		int numx = data.getNumLat();
		int numy = data.getNumLon();
		for (int i=0; i < numx; ++i) for (int j=0; j < numy; ++j){
			int in = dataval.getValue(i,j);
			if ( in != dataval.getMissing() ){
				int bin = (in - minThresh) / dataIncr;
				if ( bin > maxbin ) bin = maxbin;
				if ( bin >= 0 ){
					data.setValue(i,j, bin);
					Pixel p = new Pixel( i,j,bin );
					pixels[bin].add(p);
				}
			}
		}

		// Find the maxima. These are high-values with enough clearance
		// around them
		@SuppressWarnings("unchecked")
		List<Pixel>[] centers = new List[ maxbin+1 ];
		for (int i = 0; i < centers.length; ++i){
			centers[i] = new ArrayList<Pixel>();
		}
		List<Pixel> markedsofar = new ArrayList<Pixel>();  
		LatLonGrid marked = LatLonGrid.copyOf(data);
		marked.fill(UNMARKED);
		final int MIN_INFL = (int) Math.round(1 + 0.5*Math.sqrt(myMinSize));
		final int MAX_INFL = 2 * MIN_INFL;

		for (int bin=maxbin; bin >= 0; --bin){
			final int infl_dist = MIN_INFL + (int)Math.round(((double)bin)/maxbin * (MAX_INFL-MIN_INFL));
			for (Pixel p : pixels[bin]){
				if ( marked.getValue(p) == UNMARKED ){
					boolean ok = false;
					markedsofar.clear();
					for (int ii=p.getX()-infl_dist; ii <= p.getX()+infl_dist; ++ii)
						for (int jj=p.getY()-infl_dist; jj <= p.getY()+infl_dist; ++jj){
							int i=ii; int j=jj;
							ok = data.isValid(i,j);
							if (ok && marked.getValue(i,j) == UNMARKED){
								marked.setValue(i,j, bin);
								markedsofar.add( new Pixel(i,j,data.getValue(i,j)) );
							} else{
								// neighborhood already taken ...
								ok = false;
								break;
							}
						}
					if (ok){
						// highest point in its neighborhood
						centers[bin].add(p);
					} else {
						for (Pixel m : markedsofar){
							marked.setValue( m.getX() , m.getY() , UNMARKED);
						}
					}
				}
			}
		}
		marked.fill(UNMARKED);

		int starting_delta = 0;
		for (int delta = starting_delta; delta <= myDelta; ++delta){
			List<Pixel> deferredFromLast = new ArrayList<Pixel>();
			List<Pixel> deferredToNext = new ArrayList<Pixel>();
			for (int bin=maxbin; bin >= 0; --bin){
				int bin_lower = bin - delta;
				deferredFromLast = deferredToNext; deferredToNext.clear();
				List<Glob> foothills = new ArrayList<Glob>();
				int n_centers = centers[bin].size();
				int tot_centers = n_centers + deferredFromLast.size();
				for (int i = 0; i < tot_centers; ++i){
					// done this way to minimize memory overhead of maintaining two lists
					Pixel center = (i < n_centers)? centers[bin].get(i) : deferredFromLast.get(i-n_centers);
					if ( bin_lower < 0 ) bin_lower = 0;
					if ( marked.getValue(center) == UNMARKED ){
						boolean captured=setMaximum( data, marked, center, bin_lower , myMinSize, centers, foothills );
						if (!captured){
							// decrement to lower value to see if it'll get big enough
							Pixel defer = new Pixel( center.getX(), center.getY(), center.getValue()-1 );
							deferredToNext.add(defer);
						}
					}
				}// all centers
				System.out.println("Finished processing " + tot_centers + " potential maxima at bin=" + bin + " and delta=" + delta);
				// this is the last one for this bin
				removeFoothills( data, marked, bin, bin_lower, centers, foothills );
			} // all bins
		} // all deltas

		return marked;
	}

	public LabelResult label(LatLonGrid dataval){
	   LatLonGrid marked = findLocalMaxima(dataval);
	   LabelResult initial = new ThresholdSegmenter(0).label(marked);
	   LabelResult pruned = RegionProperty.pruneBySize(initial, dataval, myMinSize);
	   return pruned;
	}
	
	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("ewshed");
		
		// data
		LatLonGrid grid = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LinearScaling()).crop(1000, 2100, 100, 200);
		KmlWriter.write(grid, out, "orig", PngWriter.createCoolToWarmColormap());
		
		// int MIN = 200; int MAX = 500; int INCR = 10;// log scaling
		int MIN = 1; int MAX = 100; int INCR = 1;// linear scaling
		
		for (int sizethresh=5; sizethresh <= 20; sizethresh += 5){
			EnhancedWatershedSegmenter seg = new EnhancedWatershedSegmenter(MIN, INCR, MAX, sizethresh, 5);
			LatLonGrid label = seg.label(grid).label;
			KmlWriter.write(label, out, "ewsheds_"+sizethresh, PngWriter.createRandomColormap());
		}
		
		grid = new ConvolutionFilter(ConvolutionFilter.gauss(9, 9)).smooth(grid);
		for (int sizethresh=5; sizethresh <= 20; sizethresh += 5){
			EnhancedWatershedSegmenter seg = new EnhancedWatershedSegmenter(MIN, INCR, MAX, sizethresh, 5);
			LatLonGrid label = seg.label(grid).label;
			KmlWriter.write(label, out, "smewsheds_"+sizethresh, PngWriter.createRandomColormap());
		}
	}
}
