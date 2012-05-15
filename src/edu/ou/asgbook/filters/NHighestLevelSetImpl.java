/**
 * 
 */
package edu.ou.asgbook.filters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.LevelSet;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Finds the N highest valued-pixels in image
 * using a levelset implementation
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class NHighestLevelSetImpl {
	private final int nth;

	public NHighestLevelSetImpl(int nth) {
		this.nth = nth;
	}

	public Pixel[] findHighestValued(LatLonGrid input){
		// create level set
		LevelSet levelset = LevelSet.newInstance(input);
		
		// find the top n pixels
		Map.Entry<Integer, List<Pixel>>[] levels = levelset.getLevels();
		List<Pixel> result = new ArrayList<Pixel>();
		int curr = levels.length;
		while (result.size() < nth && curr > 0){
			--curr; // next
			result.addAll(levels[curr].getValue()); // all pixels at this level
		}
		
		return result.toArray(new Pixel[0]);
	}
	
	public static void main(String[] args) throws Exception {
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA);
		popdensity.setMissing(0); // will get to process less data this way
		
		// find 10 highest
		NHighestLevelSetImpl filter = new NHighestLevelSetImpl(10);
		Pixel[] result = filter.findHighestValued(popdensity);
		for (int i=0; i < result.length; ++i){
			System.out.println(i + " " + result[i] + " loc=" + popdensity.getLocation(result[i].getX(), result[i].getY()));
		}
		
		// plot the result on a map
		popdensity.fill(popdensity.getMissing());
		for (int i=0; i < result.length; ++i){
			popdensity.setValue(result[i].getX(), result[i].getY(), 1);
		}
		File out = OutputDirectory.getDefault("levelset");
		KmlWriter.write(popdensity, out, "highest10", PngWriter.createHotColormap());
		
		// plot as KML points
		List<LatLon> points = new ArrayList<LatLon>();
		List<String> names = new ArrayList<String>();
		for (int i=0; i < result.length; ++i){
			Pixel p = result[i];
			points.add( popdensity.getLocation(p.getRow(), p.getCol()));
			names.add("Pixel#"+ (i+1) );
		}
		KmlWriter.write(points, names, out, "top10pixels");
	}
}
