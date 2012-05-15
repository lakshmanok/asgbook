/**
 * 
 */
package edu.ou.asgbook.dataset;

import java.io.File;
import java.io.IOException;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.io.EsriGrid;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.linearity.LinearScaling;

/**
 * 
 * Reads night-time lights data in ESRI grid format
 * 
 * @author Valliappa.Lakshmanan
 * 
 */
public class NightimeLights {
	public static File WORLD = new File("data/nighttime/nighttimelights.txt.gz");
	
	public static LatLonGrid read(File file) throws IOException {
		return EsriGrid.read(file, new LinearScaling(100.0/63)); // 0-100
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("nighttime");
		
		// read input 
		LatLonGrid lights = NightimeLights.read(NightimeLights.WORLD);

		// write out as image, for viewing
		KmlWriter.write(lights, out, "lights", PngWriter.createCoolToWarmColormap());
	}
	
}
