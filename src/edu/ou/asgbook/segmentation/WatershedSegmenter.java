/**
 * 
 */
package edu.ou.asgbook.segmentation;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.filters.ConvolutionFilter;
import edu.ou.asgbook.filters.MaxValueFilter;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * 
 * Watershed approach of object identification.
 * 
 * @author valliappa.lakshmanan
 * 
 */
public class WatershedSegmenter implements Segmenter {
	private int thresh;

	public WatershedSegmenter(int thresh) {
		super();
		this.thresh = thresh;
	}

	/**
	 * Creates a labeled grid where background pixels are set to 0 and labels
	 * for objects go 1,2,3... All pixels > thresh are part of an object.
	 */
	public LabelResult label(LatLonGrid data) {
		return vincent_segment(data);
	}

	private boolean neighbor_is_tagged(LatLonGrid img, int x, int y) {
		for (int i = x - 1; i <= x + 1; ++i)
			for (int j = y - 1; j <= y + 1; ++j)
				if ((i != x || j != y) && img.isValid(i, j))
					if (img.getValue(i, j) >= 0)
						return true;
		return false;
	}

	private LabelResult vincent_segment(LatLonGrid img) {
		// Vincent/Soille, from IEEE PAMI June 1991.
		// initialize
		// The result image is the output image; we will slowly fill it with the
		// watershed labels
		final int WSHED = 0;
		final int INIT = -1;
		final int MASK = -2;
		final int min_valid = thresh;
		final int max_valid = new MaxValueFilter().findHighestValued(img).value;
		final int dimx = img.getNumLat();
		final int dimy = img.getNumLon();
		LatLonGrid result = LatLonGrid.copyOf(img);
		result.fill(INIT);

		// the label is the region id
		int curr_label = 0;
		// distances is the work image that contains how far away a pixel that
		// is being processed at this level is from an already tagged pixel.
		int[][] distances = new int[img.getNumLat()][img.getNumLon()];

		// sort the pixels, so as to be able to get at all the pixels
		// corresponding to particular value easily.
		@SuppressWarnings("serial")
		class PixelArray extends ArrayList<Pixel> {
		}
		PixelArray[] sorted_list = new PixelArray[max_valid - min_valid + 1];
		for (int i=0; i < sorted_list.length; ++i){
			sorted_list[i] = new PixelArray();
		}
		for (int i = 0; i < dimx; ++i)
			for (int j = 0; j < dimy; ++j)
				if (img.isValid(i, j) && img.getValue(i, j) >= min_valid) {
					// add this pixel to the appropriate pixel array
					int pos = img.getValue(i, j) - min_valid;
					sorted_list[pos].add(new Pixel(i, j, img.getValue(i, j)));
				}

		for (int val = max_valid; val >= min_valid; --val) {
			PixelArray this_list = sorted_list[val - min_valid];
			// a new queue for this processing this pixel
			Queue<Pixel> fifo = new ArrayDeque<Pixel>();

			// add pixels to the queue if their neighbors have been tagged since
			// that is information that can be used to tag those pixels also
			for (int p = 0; p < this_list.size(); ++p) {
				// In any case, set the pixel to the MASK in the output image,
				// meaning that this pixel has to be set to WSHED or a label
				// in this iteration.
				result.setValue(this_list.get(p).getX(), this_list.get(p)
						.getY(), MASK);
				if (neighbor_is_tagged(result, this_list.get(p).getX(),
						this_list.get(p).getY())) {
					distances[this_list.get(p).getX()][this_list.get(p).getY()] = 1;
					fifo.add(this_list.get(p));
				}
			}
			// Start at dist=1, then slowly work up, processing pixels that are
			// at
			// the same distance from processed pixels at the same time.
			int curr_dist = 1;

			// push in a fictitious pixel so that the first pop() works.
			// even if there were no neighbors tagged.
			// we'll use this fictitious pixel to update curr_dist
			fifo.add(new Pixel(-1, -1, -1));

			while (true) { // until queue is empty
				Pixel p = fifo.remove();
				if (p.getX() < 0) { // if it isn't valid, it is fictitious
					// If the queue is empty now, it means that every pixel at
					// this
					// level has been tagged. So, we can go to the next level.
					if (fifo.size() == 0){
						break;
					} else {
						// We have come full circle; so increment distance and
						// try again.
						++curr_dist;
						// Put the fictitious pixel back so that we can detect
						// the
						// full-circle again.
						fifo.add(new Pixel(-1, -1, -1));
						// Start processing with the next one.
						// Since there is only fictitious pixel and the queue
						// was not
						// empty when we pushed it back in, the next pop will
						// yield a
						// valid pixel.
						p = fifo.remove();
					}
				}

				// process the pixel from the queue
				for (int i = p.getX() - 1; i <= p.getX() + 1; ++i){
					for (int j = p.getY() - 1; j <= p.getY() + 1; ++j){
						if ((i != p.getX() || j != p.getY()) && img.isValid(i, j)) {
							// if a neighbor (closer than curr_dist) has a label
							if (distances[i][j] < curr_dist
									&& result.getValue(i, j) > 0) {
								// CASE a:
								// if we are untagged at this level but should
								// be tagged now
								// CASE b:
								// if we have been set earlier to WSHED in the
								// plateau test (e)
								// CASE a or b:
								// we take our neighbor's label.
								if (result.getValue(p) == MASK
										|| result.getValue(p) == WSHED){
									result.setValue(p.getX(), p.getY(),
											result.getValue(i, j));
								} else {
									// CASE c:
									// if we are tagged already, we are normally
									// okay.
									// retain old label, except for WSHED test ( d )
									// CASE d:
									// but if our tag is not the same as our
									// neighbor's,
									// tag ourselves a watershed point
									if (result.getValue(p) != result.getValue(i, j)){
										result.setValue(p.getX(), p.getY(), WSHED);
									}
								}
							} else
							// CASE e:
							// if we might be part of a plateau
							// the neighbor is WSHED and we are untagged.
							// so, we tag ourselves as WSHED
							if (result.getValue(i, j) == WSHED
									&& result.getValue(p) == MASK){
								result.setValue(p.getX(), p.getY(), WSHED);
							} else {
								// CASE f:
								// our neighbor doesn't know about himself either.
								// there is nothing we can do. We should put this
								// pixel
								// back on the queue and wait; maybe we will get a
								// better
								// deal the next time around, i.e. if some other
								// neighbor
								// can come to our rescue and take us out of the
								// queue.
								if (result.getValue(i, j) == MASK
									&& distances[i][j] == 0) {
									// this far away from already processed pixel
									distances[i][j] = curr_dist + 1;
									fifo.add(new Pixel(i, j, img.getValue(i, j)));
								}
							}
						} // neighborhood of pixel, p
					}
				}
			} // infinite loop, continues until queue is empty

			// Check if we have a new maximum
			for (int p_i = 0; p_i < this_list.size(); ++p_i) {
				Pixel p = this_list.get(p_i);
				// reset distance to zero for every point
				distances[p.getX()][p.getY()] = 0;
				if (result.getValue(p) == MASK) {
					// If the pixel p was
					// not set by all the processing above,
					// we consider it a new minimum and update the label
					++curr_label;
					// add it to a queue which is now empty.
					// we will use the queue to update our neighbors
					Queue<Pixel> fifo2 = new ArrayDeque<Pixel>();
					fifo2.add(p);
					result.setValue(p.getX(), p.getY(), curr_label);
					// we'll do this recursively by adding the neighbor to the
					// queue
					// until we reach a point where the neighbor is not MASK
					// and so can not be processed along with this pixel.
					while (fifo2.size() != 0) {
						Pixel p1 = fifo2.remove();
						for (int i = p1.getX() - 1; i <= p1.getX() + 1; ++i)
							for (int j = p1.getY() - 1; j <= p1.getY() + 1; ++j)
								if ((i != p1.getX() || j != p1.getY())
										&& img.isValid(i, j)) {
									// is a neighbor of p1
									if (result.getValue(i, j) == MASK) {
										fifo2.add(new Pixel(i, j, img.getValue(
												i, j)));
										result.setValue(i, j, curr_label);
									}
								} // for every p2 a neighbor of p1
					} // until queue is empty
				} // if pixel is still not labeled
			} // if a new minimum

		} // for every value in the range

		for (int i=0; i < dimx; ++i) for (int j=0; j < dimy; ++j){
			if ( result.getValue(i,j) < 0 ){
				result.setValue(i,j, 0);
			}
		}
		return new LabelResult( result, curr_label );
	}
	
	
	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("wshed");
		
		// data
		LatLonGrid grid = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LinearScaling()).crop(1000, 2100, 100, 200);
		KmlWriter.write(grid, out, "orig", PngWriter.createCoolToWarmColormap());
		
		// int min_thresh = 0; int max_thresh = 400; int incr_thresh = 200; // log scaling
		int min_thresh = 0; int max_thresh = 20; int incr_thresh = 10; // linear scaling
		
		for (int thresh=min_thresh; thresh <= max_thresh; thresh += incr_thresh){
			WatershedSegmenter seg = new WatershedSegmenter(thresh);
			LatLonGrid label = seg.label(grid).label;
			KmlWriter.write(label, out, "wsheds_"+thresh, PngWriter.createRandomColormap());
		}
		
		grid = new ConvolutionFilter(ConvolutionFilter.gauss(9, 9)).smooth(grid);
		for (int thresh=min_thresh; thresh <= max_thresh; thresh += incr_thresh){
			WatershedSegmenter seg = new WatershedSegmenter(thresh);
			LatLonGrid label = seg.label(grid).label;
			KmlWriter.write(label, out, "urbanareas_"+thresh, PngWriter.createRandomColormap());
		}
	}
}
