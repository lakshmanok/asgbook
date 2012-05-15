/**
 * 
 */
package edu.ou.asgbook.datamining;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.CountryPolygons;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.dataset.NightimeLights;
import edu.ou.asgbook.dataset.WorldBankGDI;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.segmentation.LabelResult;
import edu.ou.asgbook.segmentation.RegionProperty;


/**
 * 
 * The training pattern for each city
 * 
 * @author valliappa.lakshmanan
 *
 */
public class GdiPattern {
	private double[] data = new double[3];
	
	private String format(double d){
		return new DecimalFormat("0.00").format(d);
	}
	
	@Override
	public String toString(){
		return toString(" & ", "\\\\"); // for LaTeX
	}
	
	public String toString(String colsep, String linesep){
		String result = "";
		for (int i=0; i < data.length; ++i){
			String sep = (i == data.length-1)? linesep : colsep;
			result += format(data[i]) + sep;
		}
		return result;
	}
	
	public static GdiPattern[] findTrainingPattern(LabelResult cities, LatLonGrid population, LatLonGrid nightTimeLights, LatLonGrid gdiGrid) {
		// for each city, compute the other properties
		RegionProperty[] pop = RegionProperty.compute(cities, population);
		RegionProperty[] lights = RegionProperty.compute(cities, nightTimeLights);
		RegionProperty[] gdi = RegionProperty.compute(cities, gdiGrid);
		GdiPattern[] patterns = new GdiPattern[pop.length];
		for (int i=1; i < patterns.length; ++i){
			patterns[i] = new GdiPattern();
			patterns[i].data[0] = pop[i].getCval();
			patterns[i].data[1] = lights[i].getCval();
			patterns[i].data[2] = gdi[i].getCval();
		}
		return patterns;
	}
	
	public static void write(GdiPattern[] patterns, File outdir) throws IOException {
		PrintWriter writer = null;
		try {
			String name = outdir.getAbsolutePath() + "/gdipatterns.txt";
			writer = new PrintWriter( new FileWriter(name) );
			for (int i=1; i < patterns.length; ++i){
				writer.println(patterns[i].toString(" ","")); // for R
			}
		} finally {
			if (writer != null){
				writer.close();
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("gdipattern");
		final boolean SMALL = true;
		
		// read input (crop to cover Spain)
		LatLonGrid pop    = GlobalPopulation.read(GlobalPopulation.WORLD);
		if (SMALL){
			pop = pop.crop(980, 4080, 220, 350);
		}
		KmlWriter.write(pop, out, "pop", PngWriter.createRandomColormap());
		
		LatLonGrid nightTimeLights = NightimeLights.read(NightimeLights.WORLD).remapTo(pop);
		KmlWriter.write(nightTimeLights, out, "nighttimelights", PngWriter.createCoolToWarmColormap());
		
		LatLonGrid countries = CountryPolygons.readGrid(CountryPolygons.WORLD_GRID).remapTo(pop);
		KmlWriter.write(countries, out, "countries", PngWriter.createRandomColormap());
		
		LabelResult primary = PrimaryCities.findPrimaryCities(pop, countries, out);		
		KmlWriter.write(primary.label, out, "primarycities", PngWriter.createRandomColormap());

		LatLonGrid gdiGrid = WorldBankGDI.readGrid(WorldBankGDI.WORLD_GRID).remapTo(pop);
		KmlWriter.write(gdiGrid, out, "gdism", PngWriter.createCoolToWarmColormap());
		
		// obtain pattern
		GdiPattern[] patterns = GdiPattern.findTrainingPattern(primary, pop, nightTimeLights, gdiGrid);
		System.out.println("Population & Lighting & GDI \\\\");
		for (int i=1; i < patterns.length; ++i){
			System.out.println(patterns[i]);
		}
		
		if (!SMALL){
			write(patterns, out); // for R
		}
	}
	
}
