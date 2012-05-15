/**
 * 
 */
package edu.ou.asgbook.filters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Finds the N highest valued-pixels in image
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class NHighest {
	private final int n;

	public NHighest(int n) {
		this.n = n;
	}
	
	public Pixel[] findHighestValued(LatLonGrid input){
		// create array of pixels
		int[][] data = input.getData();
		final int initialCapacity = (input.getNumLat() * input.getNumLon())  / 10;
		List<Pixel> a = new ArrayList<Pixel>(initialCapacity);
		for (int i=0; i < input.getNumLat(); ++i){
			for (int j=0; j < input.getNumLon(); ++j){
				if ( data[i][j] != input.getMissing() ){
					a.add(new Pixel(i,j,data[i][j]));
				}
			}
		}
		System.out.println("Finding the " + n + " highest values out of " + a.size() + " pixels");
		
		// selection sort this array to find n highest
		Pixel[] result = new Pixel[n];
		Pixel.CompareValue comparator = new Pixel.CompareValue();
		for (int i=0; i < n; ++i){
			int p = i;
			for (int j=i; j < a.size(); ++j){
				if ( comparator.compare(a.get(j), a.get(p)) > 0 ){
					p = j;
				}
			}
			result[i] = a.get(p);
			// swap a[i] and a[p]
			Pixel temp = a.get(i);
			a.set(i, a.get(p));
			a.set(p, temp);
		}
		return result;
	}
	
	public static void main(String[] args) throws Exception {
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA);
		popdensity.setMissing(0); // will get to process less data this way
		
		// find 10 highest
		NHighest filter = new NHighest(10);
		Pixel[] result = filter.findHighestValued(popdensity);
		for (int i=0; i < result.length; ++i){
			System.out.println(i + " " + result[i] + " loc=" + popdensity.getLocation(result[i].getX(), result[i].getY()));
		}
		
		// plot the result on a map
		popdensity.fill(popdensity.getMissing());
		for (int i=0; i < result.length; ++i){
			popdensity.setValue(result[i].getX(), result[i].getY(), 1);
		}
		File out = OutputDirectory.getDefault("nhighest");
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
