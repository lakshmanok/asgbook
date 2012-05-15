/**
 * 
 */
package edu.ou.asgbook.datamining;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.dataset.NightimeLights;
import edu.ou.asgbook.dataset.WorldBankGDI;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.segmentation.EnhancedWatershedSegmenter;
import edu.ou.asgbook.segmentation.LabelResult;
import edu.ou.asgbook.segmentation.RegionProperty;


/**
 * 
 * Applies different data mining models to each city
 * 
 * @author valliappa.lakshmanan
 *
 */
public class CityGdiModels {

	public static double[][] findPatterns(LabelResult cities, LatLonGrid population, LatLonGrid nightTimeLights) {
		// make sure that this is identical to GdiPattern.java
		RegionProperty[] pop = RegionProperty.compute(cities, population);
		RegionProperty[] lights = RegionProperty.compute(cities, nightTimeLights);

		double[][] patterns = new double[pop.length][2];
		for (int i=1; i < patterns.length; ++i){
			patterns[i][0] = pop[i].getCval();
			patterns[i][1] = lights[i].getCval();
		}
		return patterns;
	}
	
	public static int[] applyLinearModel(double[][] pattern){
		int[] result = new int[ pattern.length ];
		for (int i=0; i < pattern.length; ++i){
			result[i] = (int) Math.round(0.003494 + 0.034444 * pattern[i][1] - 0.005992 * pattern[i][0]);
		}
		return result;
	}
	
	public static int[] applyDecisionTree(double[][] pattern){
		int[] result = new int[ pattern.length ];
		for (int i=0; i < pattern.length; ++i){
			double pop = pattern[i][0];
			double light = pattern[i][1];
			if (light < 48.91){
				if (light < 17.61){
					result[i] = 0;
				} else {
					result[i] = 1;
				}
			} else {
				if ( light < 81.25 ){
					if ( pop >= 31.77 ){
						result[i] = 1;
					} else {
						result[i] = 2;
					}
				} else {
					if ( pop >= 105.7 ){
						result[i] = 2;
					} else {
						result[i] = 4;
					}
				}
			}
		}
		return result;
	}
	
	private static double logistic(double ... val){
		double sum = 0;
		for (int i=0; i < val.length; ++i){
			sum += val[i];
		}
		return 1.0 / (1 + Math.exp(-sum));
	}
	private static double probOfRichNN(double pop, double light){
		// numbers from NN diagram
		double h1 = logistic( 3.06728*pop, 3.26584, 1.77153*light );
		double h2 = logistic( 0.00625*pop, 2.82917, -0.03631 * light);
		double rich = logistic(2.4291*h1, 3.12817, -11.29847*h2 );
		return rich;
	}
	public static int[] applyNeuralNetwork(double[][] pattern){
		int[] result = new int[ pattern.length ];
		for (int i=0; i < pattern.length; ++i){
			result[i] = (int) Math.round(100 * probOfRichNN(pattern[i][0], pattern[i][1]));
		}
		return result;
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("gdimodels");
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

		// compute gdi for each city
		double[][] patterns = findPatterns(allcities, pop, nightTimeLights);		
		String[] models = {"linear", "tree", "nn" };				
		for (String model : models){
			int[] modelresult = null;
			if (model.equals("linear")){
				modelresult = applyLinearModel(patterns);
			} else if (model.equals("tree")){
				modelresult = applyDecisionTree(patterns);
			} else if (model.equals("nn")){
				modelresult = applyNeuralNetwork(patterns);
			}
			LatLonGrid result = LatLonGrid.copyOf(allcities.label);
			result.setMissing(WorldBankGDI.DevelopmentCategory.Unknown.ordinal());
			result.fill( result.getMissing() );
			for (int i=0; i < result.getNumLat(); ++i){
				for (int j=0; j < result.getNumLon(); ++j){
					int cityno = allcities.label.getValue(i,j);
					if (cityno > 0 ){
						result.setValue(i, j, modelresult[cityno]);
					}
				}
			}
			KmlWriter.write(result, out, model+"gdi", PngWriter.createCoolToWarmColormap());
		}
	}
	
}
