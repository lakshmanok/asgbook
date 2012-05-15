/**
 * 
 */
package edu.ou.asgbook.usage;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.geocode.UsaZipcode;
import edu.ou.asgbook.histogram.Histogram;
import edu.ou.asgbook.histogram.OtsuThresholdSelector;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.segmentation.HysteresisSegmenter;
import edu.ou.asgbook.segmentation.LabelResult;
import edu.ou.asgbook.segmentation.RegionProperty;

/**
 * Solution to a classroom assignment to identify regions of urban
 * sprawl from the population density data.
 * 
 * @author v.lakshmanan
 *
 */
public class Sprawl {
	
	public static void runOnPopDensity(boolean crop) throws Exception {
		LatLonGrid popdensity1 = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA1990, new GlobalPopulation.LinearScaling());
		LatLonGrid popdensity2 = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LinearScaling());
		
		int x1, x2, x3, x4;
		if (crop){
			x1 = 900;
			x2 = 2500;
			x3 = 200;
			x4 = 200;
		} else {
			LatLon nwCorner = new LatLon(60, -130);
			LatLon seCorner = new LatLon(12, -52);
			x1 = popdensity1.getRow(nwCorner);
			x2 = popdensity1.getCol(nwCorner);
			x3 = popdensity1.getRow(seCorner) - popdensity1.getRow(nwCorner);
			x4 = popdensity1.getCol(seCorner) - popdensity1.getCol(nwCorner);
		}
		popdensity1 = popdensity1.crop(x1, x2, x3, x4);
		popdensity2 = popdensity2.crop(x1, x2, x3, x4);
		
		File out = OutputDirectory.getDefault("sprawl");
		
		findSprawl(popdensity1, popdensity2, out);
	}
	
	public static void findSprawl(LatLonGrid grid1, LatLonGrid grid2, File out) throws Exception {
		// write out input grids
		KmlWriter.write(grid1, out, "pop_1990", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(grid2, out, "pop_2010", PngWriter.createCoolToWarmColormap());
		System.out.println("Resolution of image = " + grid1.getLatRes() + "x" + grid1.getLonRes());
		// Find optimal threshold on 2010 data using Otsu's method
		final int MIN = 0;
		final int MAX = 400;
		final int incr = 1;
		Histogram hist = new Histogram(MIN, incr, (MAX-MIN)/incr );
		hist.update(grid2);
		OtsuThresholdSelector thresholder = new OtsuThresholdSelector(hist);
		int thresh1 = thresholder.getOptimalThreshold();
		System.out.println("Optimal threshold=" + thresh1);
		
		// A city consists of point with this threshold and contiguous points > some reasonable threshold
		int thresh2 = thresh1 * 2;
		LabelResult label1990 =  new HysteresisSegmenter(thresh1, thresh2).label(grid1);
		KmlWriter.write(label1990.label, out, "label_1990", PngWriter.createRandomColormap());
		
		LabelResult label2010 =  new HysteresisSegmenter(thresh1, thresh2).label(grid2);
		KmlWriter.write(label2010.label, out, "label_2010", PngWriter.createRandomColormap());
		
		// grow regions and find region properties
		RegionProperty[] props1 = RegionProperty.compute(label1990, grid1);
		RegionProperty[] props2 = RegionProperty.compute(label2010, grid2);
		
		// create a new grid that consists of city sizes
		LatLonGrid citysize1990 = getCitySize(label1990, props1);
		LatLonGrid citysize2010 = getCitySize(label2010, props2);
		KmlWriter.write(citysize1990, out, "citysize_1990", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(citysize2010, out, "citysize_2010", PngWriter.createCoolToWarmColormap());
		
		// compute and write out difference in size for every 2010 city
		LatLonGrid changeInSize = LatLonGrid.copyOf(citysize2010);
		Pixel[] sizechange = new Pixel[props2.length];
		for (int i=0; i < sizechange.length; ++i){
			sizechange[i] = new Pixel(0, 0,0);
		}
		for (int i=0; i < citysize2010.getNumLat(); ++i){
			for (int j=0; j < citysize2010.getNumLon(); ++j){
				int sz1 = citysize1990.getValue(i,j);
				int sz2 = citysize2010.getValue(i,j);
				int percentChange = 0;
				if (sz1 > 5 && sz2 > 5){ // reasonably big?
					percentChange = (100 * (sz2 - sz1)) / sz1;
					sizechange[ label2010.label.getValue(i,j) ] = new Pixel( i,j, percentChange);
				}
				changeInSize.setValue(i,j, percentChange);
			}
		}
		KmlWriter.write(changeInSize, out, "sprawl_1990_2010", PngWriter.createCoolToWarmColormap());
		
		// Print out the top cities
		Arrays.sort(sizechange, new Comparator<Pixel>(){
			@Override
			public int compare(Pixel arg0, Pixel arg1) {
				return arg0.getValue() - arg1.getValue();
			}
		});
		for (int i=Math.max(0,sizechange.length-20); i < sizechange.length; ++i){
			LatLon loc = citysize2010.getLocation(sizechange[i].getX(), sizechange[i].getY());
			System.out.println(  loc +
					" : " + citysize1990.getValue(sizechange[i]) +
					" to " + citysize2010.getValue(sizechange[i]) +
					"  " + getCityNear(loc)
			);
		}
		
		// Shrunk?
		System.out.println("Cities that have exhibited the least spatial growth:");
		for (int i=0; i < Math.min(20,sizechange.length); ++i){
			LatLon loc = citysize2010.getLocation(sizechange[i].getX(), sizechange[i].getY());
			System.out.println(  loc +
					" : " + citysize1990.getValue(sizechange[i]) +
					" to " + citysize2010.getValue(sizechange[i]) +
					"  " + getCityNear(loc)
			);
		}
	}

/*	
 *  Works globally ... use UsaZipcode for within USA ...
 	private static String getCityNear(LatLon loc) {
		String cityservice = "http://api.geonames.org/findNearbyPlaceName?lat=" + loc.getLat() + "&lng=" + loc.getLon() + "&username=demo";
		try {
			URL url = new URL(cityservice);
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = parser.parse(url.openStream());
			String name = document.getElementsByTagName("name").item(0).getTextContent();
			return name;
		} catch (Exception e){
			System.out.println("FAILED: " + cityservice);
		} finally {
		}
		return "";
	}*/

	private static String getCityNear(LatLon loc) {
		UsaZipcode.Entry e = UsaZipcode.getInstance().getEntryClosestTo(loc);
		if (e != null){
			return e.toString();
		}
		return " outside USA ";
	}
	
	private static LatLonGrid getCitySize(LabelResult regions, RegionProperty[] props) {
		LatLonGrid citysize = LatLonGrid.copyOf(regions.label);
		for (int i=0; i < citysize.getNumLat(); ++i){
			for (int j=0; j < citysize.getNumLon(); ++j){
				int sz = 0;
				if (citysize.getValue(i,j) > 0){
					sz = props[regions.label.getValue(i,j)].getSize();
				}
				citysize.setValue(i, j, sz);
			}
		}
		return citysize;
	}
	
	public static void main(String[] args) throws Exception {
		boolean crop = false; // if false, on USA; if true, on NYC area
		runOnPopDensity(crop);
	}
}
