/**
 * 
 */
package edu.ou.asgbook.datamining;

import java.awt.image.IndexColorModel;
import java.io.File;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.CountryPolygons;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.filters.MaxValueFilter;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.segmentation.EnhancedWatershedSegmenter;
import edu.ou.asgbook.segmentation.LabelResult;
import edu.ou.asgbook.segmentation.RegionProperty;


/**
 * 
 * Identifies the primary cities in each country
 * 
 * @author valliappa.lakshmanan
 *
 */
public class PrimaryCities {

	public static LabelResult findPrimaryCities(LatLonGrid population, LatLonGrid countries, File out) {
		// find cities from population data using watershed
		write(out, population, "pop", PngWriter.createCoolToWarmColormap());
		EnhancedWatershedSegmenter seg = new EnhancedWatershedSegmenter(10, 1, 600, 10, 5);
		LabelResult label = seg.label(population);
		RegionProperty[] popProps = RegionProperty.compute(label, population);
		write(out, label.label, "allcities", PngWriter.createRandomColormap());
		
		// initialize primary-cities
		int ncountries = 1 + new MaxValueFilter().findHighestValued(countries).value;
		int[] primaryCity = new int[ncountries]; // one for each country
		for (int i=0; i < ncountries; ++i){
			primaryCity[i] = -1; // none
		}
		
		// go through the cities and assign them to their appropriate country
		for (int i=1; i < popProps.length; ++i){
			LatLon centroid = population.getLocation(popProps[i].getCx(), popProps[i].getCy());
			int country = countries.getValue(centroid);
			if (country >= 0){
				if (primaryCity[country] < 0){
					primaryCity[country] = i; // first city in country
				} else {
					// the primary city is the one with the greater avg population
					int previous = primaryCity[country];
					if (popProps[i].getCval() > popProps[previous].getCval()){
						primaryCity[country] = i;
					}
				}
			}
		}
		
		// keep only those cities that are primary
		boolean[] keep = new boolean[popProps.length];
		for (int i=0; i < ncountries; ++i){
			if (primaryCity[i] >= 0){
				int regno = primaryCity[i];
				keep[regno] = true;
			}
		}		
		return RegionProperty.prune(label, keep);
	}
	
	private static void write(File out, LatLonGrid label, String dataName, IndexColorModel colormap) {
		try {
			if (out != null){
				KmlWriter.write(label, out, dataName, colormap);
			}
		} catch (Exception e){
			
		}
	}

	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("primary");
		
		// read input (crop to cover Spain)
		LatLonGrid pop    = GlobalPopulation.read(GlobalPopulation.WORLD).crop(980, 4080, 220, 350);
		LatLonGrid countries = CountryPolygons.readGrid(CountryPolygons.WORLD_GRID);
		LabelResult primary = PrimaryCities.findPrimaryCities(pop, countries, out);
		
		KmlWriter.write(primary.label, out, "primarycities", PngWriter.createRandomColormap());
	}
	
}
