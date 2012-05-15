/**
 * 
 */
package edu.ou.asgbook.usage;

import java.io.File;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.distance.EuclideanDTSaito;
import edu.ou.asgbook.filters.Inverter;
import edu.ou.asgbook.filters.SimpleThresholder;
import edu.ou.asgbook.histogram.Histogram;
import edu.ou.asgbook.histogram.OtsuThresholdSelector;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * (1) Find optimal threshold on log(pop)
 * Find distance of every grid point to a point < thresh
 * Find optimal threshold of distance values
 * Threshold image to keep only values < threshold
 * @author v.lakshmanan
 *
 */
public class Assignment4 {

	public static void main(String[] args) throws Exception {
		File outdir = OutputDirectory.getDefault("assignment4");
		
		// read input
		LatLon nwCorner = new LatLon(60, -130);
		LatLon seCorner = new LatLon(7, -52);
		// LatLonGrid conus = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling());
		LatLonGrid conus = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA);
		conus = conus.crop(conus.getRow(nwCorner),
				conus.getCol(nwCorner),
				conus.getRow(seCorner) - conus.getRow(nwCorner),
				conus.getCol(seCorner) - conus.getCol(nwCorner));
		KmlWriter.write(conus, outdir, "orig", PngWriter.createCoolToWarmColormap());
		
		// find threshold
		int popthresh = -1;
		{
			final int MIN = 0;
			final int MAX = 500;
			final int incr = 10;
			Histogram hist = new Histogram(MIN, incr, (MAX-MIN)/incr );
			hist.update(conus);
			popthresh = new OtsuThresholdSelector(hist).getOptimalThreshold();
			System.out.println("Optimal population threshold=" + popthresh);
		}
		
		// threshold
		//LatLonGrid threshed = new SimpleThresholder(popthresh).filter(conus);
		//KmlWriter.write(threshed, outdir, "thresh", PngWriter.createCoolToWarmColormap());
		
		// distance to points > thresh
		LatLonGrid distToCity = new EuclideanDTSaito().getDistanceTransform(conus, popthresh);
		KmlWriter.write(distToCity, outdir, "distToCity", PngWriter.createCoolToWarmColormap());
		
		// optimal threshold on distance
		int distthresh = -1;
		{
			final int MIN = 0;
			final int MAX = 10000;
			final int incr = 10;
			Histogram hist = new Histogram(MIN, incr, (MAX-MIN)/incr );
			hist.update(distToCity);
			distthresh = new OtsuThresholdSelector(hist).getOptimalThreshold();
			System.out.println("Optimal distance threshold=" + distthresh);
		}
		
		// threshold by distance to find metropolitan areas
		LatLonGrid boondocks = new SimpleThresholder(distthresh/2).filter(distToCity);
		LatLonGrid metros = new Inverter(1).filter(boondocks);
		KmlWriter.write(metros, outdir, "metros", PngWriter.createCoolToWarmColormap());
		
		
	}
}
