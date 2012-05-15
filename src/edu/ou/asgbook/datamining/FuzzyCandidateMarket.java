/**
 * 
 */
package edu.ou.asgbook.datamining;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.ou.asgbook.core.LatLon;
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
 * 
 * Uses heuristic rules to choose the next market to enter
 * 
 * @author valliappa.lakshmanan
 *
 */
public class FuzzyCandidateMarket {
	private final FuzzyLogic.Rule lightHigh;
	private final FuzzyLogic.Rule populationSparse;
	private final FuzzyLogic.Rule populationHigh;
	
	public FuzzyCandidateMarket(){
		lightHigh = new FuzzyLogic.IsHigh(30, 70);
		populationSparse = new FuzzyLogic.IsLow(5,10);
		populationHigh = new FuzzyLogic.IsHigh(30,80);
	}
	
	// 0-10
	public int isGoodCandidate(double population, double lightIntensity){
		// apply the basic rules
		FuzzyLogic.Fuzzy highlight = lightHigh.apply(lightIntensity);
		FuzzyLogic.Fuzzy popSparse = populationSparse.apply(population);
		FuzzyLogic.Fuzzy popHigh = populationHigh.apply(population);

		// if high light and moderate population density ...
		FuzzyLogic.Fuzzy popModerate = popSparse.not().and( popHigh.not() );	
		FuzzyLogic.Fuzzy result = popModerate.and(highlight);
		
		return (int) Math.round(result.getValue()*10);
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("fuzzy");
		
		// read input (crop to cover Spain)
		LatLonGrid lights = NightimeLights.read(NightimeLights.WORLD).crop(980, 4080, 220, 350);
		LatLonGrid pop    = GlobalPopulation.read(GlobalPopulation.WORLD).crop(980, 4080, 220, 350);
		
		// sanity check: are both grids correctly geolocated?
		System.out.println("Lights nwcorner: " + lights.getNwCorner());
		System.out.println("Population nwcorner: " + pop.getNwCorner());
		
		// apply fuzzy logic
		FuzzyCandidateMarket rules = new FuzzyCandidateMarket();
		LatLonGrid result = LatLonGrid.copyOf(lights);
		result.fill(0);
		result.setMissing(0);
		for (int i=0; i < result.getNumLat(); ++i){
			for (int j=0; j < result.getNumLon(); ++j){
				result.setValue(i,j, rules.isGoodCandidate(pop.getValue(i,j), lights.getValue(i,j)));
			}
		}
		
		// write out as image, for viewing
		KmlWriter.write(lights, out, "fzlights", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(pop, out, "fzpop", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(result, out, "candidatepixels", PngWriter.createCoolToWarmColormap());
		
		// find cities from population data using watershed
		EnhancedWatershedSegmenter seg = new EnhancedWatershedSegmenter(10, 1, 130, 10, 5);
		LabelResult label = seg.label(pop);
		RegionProperty[] popProps = RegionProperty.compute(label, pop);
		RegionProperty[] lightProps = RegionProperty.compute(label, lights);

		List<LatLon> points = new ArrayList<LatLon>();
		List<String> names = new ArrayList<String>();
		int[] howgood = new int[popProps.length];
		for (int i=1; i < howgood.length; ++i){
			howgood[i] = rules.isGoodCandidate(popProps[i].getCval(), lightProps[i].getCval());
			if (howgood[i] > 5){
				points.add( result.getLocation(popProps[i].getCx(), popProps[i].getCy()) );
				names.add( " " + howgood[i]);
				System.out.println( points.get(points.size()-1) + " " + howgood[i]);
			}
		}
		KmlWriter.write(points, names, out, "candidates");
		
		LatLonGrid candidateCities = LatLonGrid.copyOf(result);
		for (int i=0; i < candidateCities.getNumLat(); ++i){
			for (int j=0; j < candidateCities.getNumLon(); ++j){
				int regno = label.label.getValue(i,j);
				if (regno > 0){
					candidateCities.setValue(i, j, howgood[regno]);
				} else {
					candidateCities.setValue(i, j, 0);
				}
			}
		}
		KmlWriter.write(candidateCities, out, "candidateCities", PngWriter.createCoolToWarmColormap());
	}
	
}
