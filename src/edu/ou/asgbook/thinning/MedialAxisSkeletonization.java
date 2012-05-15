/**
 * 
 */
package edu.ou.asgbook.thinning;

import java.io.File;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.distance.EuclideanDTSaito;
import edu.ou.asgbook.filters.DilateErodeFilter;
import edu.ou.asgbook.filters.ErodeDilateFilter;
import edu.ou.asgbook.filters.Inverter;
import edu.ou.asgbook.filters.SimpleThresholder;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * The MAT method of skeletonizing a grid.
 * 
 * @author v.lakshmanan
 * 
 */
public class MedialAxisSkeletonization {
	
	public static LatLonGrid findSkeleton(LatLonGrid input, int thresh, File out) throws Exception {
		// threshold and invert
		LatLonGrid binaryImage = new SimpleThresholder(thresh).threshold(input);
		if (out != null){
			KmlWriter.write(binaryImage, out, "thresh", PngWriter.createCoolToWarmColormap());
		}
		binaryImage = new Inverter(1).invert(binaryImage);
		
		// compute distance to pts > 0 i.e. boundary pixels
		LatLonGrid edt = new EuclideanDTSaito().getDistanceTransform(binaryImage, 0);
		if (out != null){
			KmlWriter.write(edt, out, "edt", PngWriter.createCoolToWarmColormap());
		}
		
		// retain local maximum in 4-neighborhood
		LatLonGrid result = new LatLonGrid(edt.getNumLat(),edt.getNumLon(),edt.getMissing(),edt.getNwCorner(),edt.getLatRes(),edt.getLonRes());
		for (int i=1; i < edt.getNumLat()-1; ++i){
			for (int j=1; j < edt.getNumLon()-1; ++j){
				int edtval = edt.getValue(i, j);
				if ( edtval != 0 &&
					 edt.getValue(i-1,j) <= edtval &&
					 edt.getValue(i,j-1) <= edtval &&
					 edt.getValue(i+1,j) <= edtval &&
					 edt.getValue(i,j+1) <= edtval ){
					result.setValue(i,j, 1);
				} else {
					result.setValue(i,j, 0);
				}
			}
		}
		return result;
	}
	
	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("matskeleton");
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LogScaling()).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "orig", PngWriter.createCoolToWarmColormap());

		popdensity = new DilateErodeFilter(2,3).filter(popdensity);
		// popdensity = new ErosionFilter(3).filter(popdensity);
		popdensity = new ErodeDilateFilter(2,3).filter(popdensity);
		KmlWriter.write(popdensity, out, "filledin", PngWriter.createCoolToWarmColormap());
		
		LatLonGrid result = findSkeleton(popdensity, 300, out);
		result.setMissing(0); // to make the 1s pop out
		KmlWriter.write(result, out, "mat", PngWriter.createCoolToWarmColormap());
	}
}
