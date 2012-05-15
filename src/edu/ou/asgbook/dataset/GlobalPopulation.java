/**
 * 
 */
package edu.ou.asgbook.dataset;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.io.EsriGrid;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.linearity.DataTransform;

/**
 * 
 * Reads the ASCII population data available at
 * http://sedac.ciesin.columbia.edu/gpw
 * 
 * @author Valliappa.Lakshmanan
 * 
 */
public class GlobalPopulation {
	public static class LogScaling extends edu.ou.asgbook.linearity.LogScaling {
		public LogScaling(){
			super(100);
		}
	}

	public static class LinearScaling extends edu.ou.asgbook.linearity.LinearScaling {
		public LinearScaling(){
			super(0.001);
		}
	}
	
	public static LatLonGrid read(Reader inputFile, DataTransform t) {
		return EsriGrid.read(inputFile, t);
	}

	public static File WORLD = new File("data/popdensity/glp10ag.asc.gz");
	public static File NORTHAMERICA = new File("data/popdensity/nap10ag.asc.gz");
	public static File NORTHAMERICA1990 = new File("data/popdensity/nap90ag.asc.gz");
	
	/**
	 * reads data from a File. The File can be gzipped or uncompressed.
	 */
	public static LatLonGrid read(File file, DataTransform t) throws IOException {
		return EsriGrid.read(file, t);
	}

	public static LatLonGrid read(File file) throws IOException {
		return read(file, new LinearScaling());
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("globalpop");
		
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA).crop(900, 2500, 200, 200);
		
		// write out as image, for viewing
		KmlWriter.write(popdensity, out, "popdensity", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(GlobalPopulation.read(GlobalPopulation.WORLD, new LogScaling()), out, "globaldensity", PngWriter.createCoolToWarmColormap());
		
		// show impact of colormap and log scaling
		KmlWriter.write(popdensity, out, "rainbow", PngWriter.createHotColormap());		
		popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new LogScaling()).crop(900, 2500, 200, 200);
		KmlWriter.write(popdensity, out, "logdensity", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(popdensity, out, "lograinbow", PngWriter.createHotColormap());
	}
	
}
