/**
 * 
 */
package edu.ou.asgbook.datamining;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.dataset.NightimeLights;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.segmentation.EnhancedWatershedSegmenter;
import edu.ou.asgbook.segmentation.LabelResult;
import edu.ou.asgbook.segmentation.RegionProperty;


/**
 * Obtains city data for clustering
 * 
 * @author valliappa.lakshmanan
 *
 */
public class CityCategories {
		
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("citycategories");
		final boolean SMALL = true;
		
		// read input (crop to cover China)
		LatLonGrid pop    = GlobalPopulation.read(GlobalPopulation.WORLD);
		if (SMALL){
			pop = pop.crop(900, 6000, 800, 1600); // China mainly
		}
		KmlWriter.write(pop, out, "modelpop", PngWriter.createRandomColormap());
		
		LatLonGrid nightTimeLights = NightimeLights.read(NightimeLights.WORLD).remapTo(pop);
		KmlWriter.write(nightTimeLights, out, "modellights", PngWriter.createCoolToWarmColormap());
		
		EnhancedWatershedSegmenter seg = new EnhancedWatershedSegmenter(10, 1, 600, 10, 5);
		LabelResult allcities = seg.label(pop);		
		KmlWriter.write(allcities.label, out, "modelcities", PngWriter.createRandomColormap());

		// write out cluster file
		String filename = out.getAbsolutePath()+"/citydata.txt";
		PrintWriter writer = new PrintWriter(new FileWriter(filename));
		writer.println("Pop light");
		RegionProperty[] population = RegionProperty.compute(allcities, pop);
		RegionProperty[] lighting = RegionProperty.compute(allcities, nightTimeLights);
		for (int i=1; i < population.length; ++i){
			writer.println(population[i].getCval() + " " + lighting[i].getCval());
		}
		writer.close();
		System.out.println("Wrote " + filename);
		
		// compute the category of each (based on clustering result)
		int[] categories = new int[population.length];
		for (int i=1; i < categories.length; ++i){
			categories[i] = computeCategory( population[i].getCval(), lighting[i].getCval() );
		}
		LatLonGrid result = LatLonGrid.copyOf(allcities.label);
		result.setMissing(0);
		result.fill(result.getMissing());
		for (int i=0; i < result.getNumLat(); ++i){
			for (int j=0; j < result.getNumLon(); ++j){
				int cityno = allcities.label.getValue(i,j);
				if ( cityno > 0 ){
					result.setValue(i,j, categories[cityno]);
				}
			}
		}
		KmlWriter.write(result, out, "citycategories", PngWriter.createCoolToWarmColormap());
	}

	private static double[][] centers = new double[][]{
		{4.0449725  , 1.1114502},
		{-0.2898599 , -0.7153647},
		{0.1143215  , 1.0642556}
	};
	private static int computeCategory(double pop, double light) {
		// scale the two values
		double s1 = (pop - 32.6673)/33.05260;
		double s2 = (light - 42.5539)/31.75972;
		double mindistsq = Integer.MAX_VALUE;
		int category = 0;
		for (int i=0; i < centers.length; ++i){
			double dist1 = (s1 - centers[i][0]);
			double dist2 = (s2 - centers[i][1]);
			double distsq = dist1*dist1 + dist2*dist2;
			if (distsq < mindistsq){
				category = i+1;
				mindistsq = distsq;
			}
		}
		return category;
	}
	
}
