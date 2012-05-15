/**
 * 
 */
package edu.ou.asgbook.transforms;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.filters.DilateErodeFilter;
import edu.ou.asgbook.filters.ErosionFilter;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.thinning.HilditchSkeletonization;

/**
 * 
 * Finds lines in image
 * 
 * @author v.lakshmanan
 * 
 */
public class HoughTransform {
	private int DELTA_THETA = 20;
	private int DELTA_RHO = 10;
	
	public static class Line implements Comparable<Line> {
		private double rho, theta;
		private int x1 = Integer.MAX_VALUE, x2 = Integer.MIN_VALUE;
		private int y1 = Integer.MAX_VALUE, y2 = Integer.MIN_VALUE;
		private int numVotes = 0;

		@Override
		public String toString(){
			return numVotes + " & (" + x1 + "," + y1 +
			") & (" + x2 + "," + y2 + ") \\\\";
		}
		
		@Override
		/** orders it so that higher votes come first in sort order. */
		public int compareTo(Line other) {
			return other.getQuality() - this.getQuality();
		}

		/** quality of a line: how many votes vs. its length. */
		public int getQuality(){
			if ( numVotes > 1 ){
				double length = Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
				return (int) Math.round( numVotes * numVotes / length );
			} else {
				return 0;
			}
		}
		
		public List<Pixel> computePixels(int maxx, int maxy){
			List<Pixel> pixels = new ArrayList<Pixel>();
			if (numVotes < 2){
				return pixels;
			}
			int lenx = x2 - x1;
			int leny = y2 - y1;
			double costheta = Math.cos(theta);
			double sintheta = Math.sin(theta);

			if (lenx > leny){
				// iterate in x
				for (int x=x1; x <= x2; ++x){
					int y = (int) Math.round( (rho - x*costheta) / sintheta );
					if ( y >= 0 && y < maxy ){
						pixels.add(new Pixel(x,y,0));
					}
				}
			} else {
				// iterate in y
				for (int y=y1; y <= y2; ++y){
					int x = (int) Math.round( (rho - y*sintheta) / costheta );
					if ( x >= 0 && x < maxx ){
						pixels.add(new Pixel(x,y,0));
					}
				}
			}
			return pixels;
		}
		
	}

	/**
	 * Find best lines that connect points > thresh
	 * 
	 * @param grid
	 * @param datathresh
	 * @return
	 */
	public Line[] findLines(LatLonGrid grid, int datathresh) {
		int maxr = grid.getNumLat() + grid.getNumLon();
		int numr = (int) Math.round(maxr / DELTA_RHO);
		int numtheta = (int) Math.round(360 / DELTA_THETA);

		// update vote
		Line[] lines = new Line[numr * numtheta];
		for (int i=0; i < lines.length; ++i){
			lines[i] = new Line();
		}
		for (int i = 0; i < grid.getNumLat(); ++i) {
			for (int j = 0; j < grid.getNumLon(); ++j) {
				if (grid.getValue(i, j) > datathresh) {
					// use this point to cast votes ...
					for (int theta = 0; theta < numtheta; ++theta) {
						double theta_radians = (theta * DELTA_THETA * Math.PI) / 180.0;
						double rho = i * Math.cos(theta_radians) + j
								* Math.sin(theta_radians);
						int r = (int) Math.round(rho / DELTA_RHO);
						if (r >= 0 && r < maxr) {
							Line line = lines[r * numtheta + theta];
							line.rho = rho;
							line.theta = theta_radians;
							line.numVotes++;
							line.x1 = Math.min(line.x1, i);
							line.x2 = Math.max(line.x2, i);
							line.y1 = Math.min(line.y1, j);
							line.y2 = Math.max(line.y2, j);
						}
					}
				}
			}
		}

		// sort the lines by vote
		Arrays.sort(lines);
		return lines;
	}

	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("hough");

		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());

		// fill in
		popdensity = new DilateErodeFilter(2,3).filter(popdensity);
		popdensity = new ErosionFilter(3).filter(popdensity);
		KmlWriter.write(popdensity, out, "filledin", PngWriter.createCoolToWarmColormap());
		
		// skeletonize
		LatLonGrid skel = HilditchSkeletonization.findSkeleton(popdensity, 300, out);
		KmlWriter.write(skel, out, "skel", PngWriter.createCoolToWarmColormap());

		// find lines
		HoughTransform hough = new HoughTransform();
		HoughTransform.Line[] lines = hough.findLines(skel, 0);
		final int NBEST = 3;
		for (int i=0; i < Math.min(lines.length,NBEST); ++i){ // NBEST lines
			HoughTransform.Line line = lines[i];
			System.out.println(line);
			List<Pixel> pixels = line.computePixels(popdensity.getNumLat(), popdensity.getNumLon());
			for (Pixel p : pixels){
				popdensity.setValue(p.getX(), p.getY(), 1000);
			}
		}
		KmlWriter.write(popdensity, out, "lines",
				PngWriter.createCoolToWarmColormap());
	}
}
