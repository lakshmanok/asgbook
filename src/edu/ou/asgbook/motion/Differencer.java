/**
 * 
 */
package edu.ou.asgbook.motion;

import java.io.File;
import java.util.Date;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pair;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.dataset.SeviriInfraredTemperature;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.linearity.DataTransform;

/**
 * Just computes a pixel-by-pixel difference.
 * This is really not a motion estimation, but is there
 * just to show what happens when you do so.
 * @author v.lakshmanan
 *
 */
public class Differencer {
	public LatLonGrid compute(LatLonGrid data0, LatLonGrid data1){
		LatLonGrid result = LatLonGrid.copyOf(data1);
		for (int i=0; i < result.getNumLat(); ++i){
			for (int j=0; j < result.getNumLon(); ++j){
				int diff = data1.getValue(i,j) - data0.getValue(i,j);
				result.setValue(i,j, diff);
			}
		}
		return result;
	}
	
	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("difference");
		
		// seviri
		File f = new File("data/seviri");
		Pair<LatLonGrid,Date>[] grids = SeviriInfraredTemperature.readAll(f);
		KmlWriter.write(grids[0].first, out, "ir0", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(grids[1].first, out, "ir1", PngWriter.createCoolToWarmColormap());
		LatLonGrid diff = new Differencer().compute(grids[0].first, grids[1].first);
		KmlWriter.write(diff, out, "irdiff", PngWriter.createCoolToWarmColormap());
		
		// popdensity
		DataTransform[] transforms = {new GlobalPopulation.LinearScaling(), new GlobalPopulation.LogScaling()};
		String[] prefix = {"popdensity", "logpopdensity"};
		for (int i=0; i < transforms.length; ++i){
			LatLonGrid popdensity0 = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA1990, transforms[i]).crop(900, 2500, 200, 200);
			LatLonGrid popdensity1 = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, transforms[i]).crop(900, 2500, 200, 200);
			KmlWriter.write(popdensity0, out, prefix[i]+"0", PngWriter.createCoolToWarmColormap());
			KmlWriter.write(popdensity0, out, prefix[i]+"1", PngWriter.createCoolToWarmColormap());
			diff = new Differencer().compute(popdensity0, popdensity1);
			KmlWriter.write(diff, out, prefix[i]+"diff", PngWriter.createCoolToWarmColormap());
		}
	}
}
