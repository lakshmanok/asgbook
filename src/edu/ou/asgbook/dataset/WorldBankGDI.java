/**
 * 
 */
package edu.ou.asgbook.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.io.EsriGrid;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.linearity.LinearScaling;

/**
 * Reads country-by-country Global development index from World Bank
 * 
 * @author v.lakshmanan
 *
 */
public class WorldBankGDI {
	public static File WORLD_TABULAR = new File("data/development/WDI_GDF_Country.csv");	
	public static File WORLD_GRID = new File("data/development/globaldevelopmentindex.txt.gz");
	
	public enum DevelopmentCategory {
		LowIncome, LowerMiddleIncome, UpperMiddleIncome, HighIncomeNonOECD, HighIncomeOECD, Unknown;

		public static DevelopmentCategory getInstance(String text){
			if (text.equals("Low income")){
				return LowIncome;
			} else 	if (text.equals("Lower middle income")){
				return LowerMiddleIncome;
			} else 	if (text.equals("Upper middle income")){
				return UpperMiddleIncome;
			} else 	if (text.equals("High income: nonOECD")){
				return HighIncomeNonOECD;
			} else 	if (text.equals("High income: OECD")){
					return HighIncomeOECD;
			}
			throw new IllegalArgumentException("Unknown category: {" + text + "}");
		}
	}
	
	public static class CountryDI {
		public final String name;
		public final DevelopmentCategory category;
		public CountryDI(String name, String category) {
			this.name = name;
			this.category = DevelopmentCategory.getInstance(category);
		}
		@Override
		public String toString(){
			return name + " -- " + category;
		}
	}
	
	/**
	 * reads data from a CSV File. The File can be gzipped or uncompressed.
	 */
	public static CountryDI[] read(File file) throws IOException {
		Reader f = null;
		System.out.println("Reading " + file.getAbsolutePath());
		if (file.getAbsolutePath().endsWith(".gz")) {
			f = new InputStreamReader(new GZIPInputStream(new FileInputStream(
					file)));
		} else {
			f = new FileReader(file);
		}
		return read(f);
	}
	
	/**
	 * reads data from a ESRI grid file. The File can be gzipped or uncompressed.
	 */
	public static LatLonGrid readGrid(File file) throws IOException {
		return EsriGrid.read(file, new LinearScaling(1));
	}
	
	private static CountryDI[] read(Reader f) throws IOException {
		BufferedReader reader = null;
		List<CountryDI> countries = new ArrayList<CountryDI>();
		int lineno = 1;
		try {
			reader = new BufferedReader(f);
			String line = reader.readLine(); // skip first line
			while ((line = reader.readLine()) != null ){
				++lineno;
				String[] cols = line.split(",");
				String cat = cols[4];
				if ( cat.length() > 0 && !cat.equals("Aggregates") ){
					CountryDI c = new CountryDI(cols[1], cat);
					// System.out.println(lineno + ":" + c);
					countries.add(c);
				}
			}
		} catch (Exception e){
			System.err.println("Error " + e.getMessage() + " at line#" + lineno);
			System.exit(-1);
		} finally {
			if (reader != null)
				reader.close();
		}
		
		System.out.println("Successfully read in development category for " + countries.size() + " countries");
		return countries.toArray(new CountryDI[0]);
	}
	
	public static class DevelopmentLookup {
		private Map<String, CountryDI> lookup = new TreeMap<String, CountryDI>();
		public void add(CountryDI c){
			// by name
			lookup.put(c.name, c);
		}
		public CountryDI get(String name){
			CountryDI match = lookup.get(name);
			if (match != null){
				return match;
			}
			// try just the first part of the name
			int matchlen = name.length() / 2;
			String tomatch = name.substring(0,matchlen);
			for (Map.Entry<String,CountryDI> entry : lookup.entrySet()){
				if (entry.getKey().startsWith(tomatch)){
					return entry.getValue();
				}
			}
			return null;
		}
				
		public DevelopmentCategory[] getDevelopmentCategories(CountryPolygons.Country[] countries){
			DevelopmentCategory[] cats = new DevelopmentCategory[countries.length];
			for (int i=0; i < countries.length; ++i){
				CountryDI c = get(countries[i].name);
				if (c == null){
					cats[i] = DevelopmentCategory.Unknown;
				} else {
					cats[i] = c.category;
				}
			}
			return cats;
		}
	}
	
	public static DevelopmentLookup readAsMap(File f) throws Exception {
		DevelopmentLookup result = new DevelopmentLookup();
		for (CountryDI c : read(f)){
			result.add(c);
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		WorldBankGDI.CountryDI[] countries = WorldBankGDI.read(WorldBankGDI.WORLD_TABULAR);
		for (WorldBankGDI.CountryDI c : countries){
			System.out.println(c);
		}
		
		// some basic stats
		System.out.println("Distribution of income levels is as follows:");
		int[] count = new int[ DevelopmentCategory.values().length ];
		for (WorldBankGDI.CountryDI c : countries){
			count[ c.category.ordinal() ] ++;
		}
		for (int i=0; i < count.length; ++i){
			WorldBankGDI.DevelopmentCategory cat = WorldBankGDI.DevelopmentCategory.values()[i];
			System.out.println( cat + " " + count[i]);
		}
		
		// now combine with country polygons
		DevelopmentLookup lookup = WorldBankGDI.readAsMap(WorldBankGDI.WORLD_TABULAR);
		LatLonGrid countryGrid = CountryPolygons.readGrid(CountryPolygons.WORLD_GRID);
		DevelopmentCategory[] categories = lookup.getDevelopmentCategories(CountryPolygons.readKml(CountryPolygons.WORLD_KML));
		for (int i=0; i < countryGrid.getNumLat(); ++i){
			for (int j=0; j < countryGrid.getNumLon(); ++j){
				int countryIndex = countryGrid.getValue(i,j);
				if ( countryIndex >= 0 ){
					int devCategory = categories[countryIndex].ordinal();
					countryGrid.setValue(i,j, devCategory);
				}
			}
		}
		File out = OutputDirectory.getDefault("countries");
		KmlWriter.write(countryGrid, out, "gdi", PngWriter.createCoolToWarmColormap());
		EsriGrid.write(countryGrid, out, "globaldevelopmentindex.txt.gz");
		EsriGrid.write(countryGrid, WorldBankGDI.WORLD_GRID);
	}
}
