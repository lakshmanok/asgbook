/**
 * 
 */
package edu.ou.asgbook.thinning;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.filters.DilateErodeFilter;
import edu.ou.asgbook.filters.ErodeDilateFilter;
import edu.ou.asgbook.filters.SimpleThresholder;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Hilditch method of skeletonizing a grid.
 * 
 * @author v.lakshmanan
 * 
 */
public class HilditchSkeletonization {
	private static class State {
		int prev;
		int ap;
		int bp;
		State(int prev){
			this.prev = prev;
			ap = bp = 0;
		}
		void update(int now){
			if (now > 0){
				++bp;
				if (prev == 0) ++ap; // 0->1 transition
			}
			prev = now;
		}
	}
	public static LatLonGrid findSkeleton(LatLonGrid input, int thresh, File out) throws Exception {
		// threshold.  object=1 and background=0
		LatLonGrid binaryImage = new SimpleThresholder(thresh).threshold(input);
		if (out != null){
			KmlWriter.write(binaryImage, out, "thresh", PngWriter.createCoolToWarmColormap());
		}
		
		final int nx = binaryImage.getNumLat();
		final int ny = binaryImage.getNumLon();
		int numChanges;
		do {
			// compute ap, bp
			LatLonGrid ap = new LatLonGrid(nx,ny,-1,binaryImage.getNwCorner(),binaryImage.getLatRes(),binaryImage.getLonRes());
			LatLonGrid bp = new LatLonGrid(nx,ny,-1,binaryImage.getNwCorner(),binaryImage.getLatRes(),binaryImage.getLonRes());
			for (int i=1; i < (nx-1); ++i) for (int j=1; j < (ny-1); ++j){
				if ( binaryImage.getValue(i, j) > 0){
					// find A(p) and B(p)
					State state = new State( binaryImage.getValue(i-1,j-1) );
					state.update( binaryImage.getValue(i-1, j) );
					state.update( binaryImage.getValue(i-1, j+1) );
					state.update( binaryImage.getValue(i, j+1) );
					state.update( binaryImage.getValue(i+1, j+1) );
					state.update( binaryImage.getValue(i+1, j) );
					state.update( binaryImage.getValue(i+1, j-1) );
					state.update( binaryImage.getValue(i, j-1) );
					state.update( binaryImage.getValue(i-1, j-1) );
					ap.setValue(i,j, state.ap);
					bp.setValue(i,j, state.bp);
				}
			}
			
			// peel off pixel?
			numChanges = 0;
			LatLonGrid after = LatLonGrid.copyOf(binaryImage);
			for (int i=1; i < (nx-1); ++i) for (int j=1; j < (ny-1); ++j){
				if ( ap.getValue(i,j) == 1 && bp.getValue(i,j) >= 2 && bp.getValue(i,j) <= 6){
					if ( ap.getValue(i-1,j) == 0 ||
							binaryImage.getValue(i-1,j) == 0 ||
							binaryImage.getValue(i,j+1) == 0 ||
							binaryImage.getValue(i,j-1) == 0 ){
						if ( ap.getValue(i,j+1) == 0 ||
								binaryImage.getValue(i-1,j) == 0 ||
								binaryImage.getValue(i,j+1) == 0 ||
								binaryImage.getValue(i+1,j) == 0){
							// peel
							after.setValue(i,j, 0);
							++numChanges;
						}
					}
				}
			}
			binaryImage = after;
			System.out.println(numChanges + " pixels peeled off in iteration");
		} while (numChanges > 0);

		return binaryImage;
	}
	
	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("hilditchskeleton");
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());

		popdensity = new DilateErodeFilter(2,3).filter(popdensity);
		popdensity = new ErodeDilateFilter(2,3).filter(popdensity);
		KmlWriter.write(popdensity, out, "filledin", PngWriter.createCoolToWarmColormap());
		
		LatLonGrid result = findSkeleton(popdensity, 300, out);
		result.setMissing(0); // to make the 1s pop out
		KmlWriter.write(result, out, "skel", PngWriter.createCoolToWarmColormap());
	}
}
