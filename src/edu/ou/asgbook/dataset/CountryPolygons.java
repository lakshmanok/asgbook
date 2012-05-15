/**
 * 
 */
package edu.ou.asgbook.dataset;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.io.EsriGrid;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.linearity.LinearScaling;
import edu.ou.asgbook.rasterization.BoundingBox;
import edu.ou.asgbook.rasterization.Polygon;

/**
 * Reads country-by-country coordinates from a KML placemarks file
 * 
 * @author v.lakshmanan
 *
 */
public class CountryPolygons {
	public static File WORLD_KML = new File("data/countries/countries_world.kml");	
	public static File WORLD_GRID = new File("data/countries/countries_world.txt.gz");	
	
	public static class Country {
		public final String name;
		public final List<Polygon> polygon;
		private BoundingBox boundingBox;
		
		public Country(String name, List<Polygon> polygon) {
			this.name = name;
			this.polygon = polygon;
			this.boundingBox = BoundingBox.copyOf(polygon.get(0).getBoundingBox());
			for (Polygon p : polygon){
				this.boundingBox.update(p.getBoundingBox());
			}
		}

		public boolean contains(LatLon pt){
			if (this.boundingBox.contains(pt.getLat(), pt.getLon())){
				for (Polygon p : polygon){
					if (p.contains(pt.getLat(), pt.getLon())){
						return true;
					}
				}
			}
			return false;
		}
		
		@Override
		public String toString(){
			return name + " has " + polygon.size() + " polygons";
		}
	}
	
	/**
	 * reads data from a File. The File can be gzipped or uncompressed.
	 */
	public static Country[] readKml(File file) throws Exception {
		InputStream f = null;
		System.out.println("Reading " + file.getAbsolutePath());
		f = new FileInputStream(file);
		if (file.getAbsolutePath().endsWith(".gz")) {
			f = new GZIPInputStream(f);
		}
		return readKml(f);
	}
	
	public static LatLonGrid readGrid(File file) throws Exception {
		return EsriGrid.read(file, new LinearScaling(1));
	}
	
	private static Country[] readKml(InputStream f) throws Exception {
		List<Country> countries = new ArrayList<Country>();
		try {
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = parser.parse(f);
			NodeList placemarks = doc.getElementsByTagName("Placemark");
			System.out.println("Saw " + placemarks.getLength() + " countries.");
			for (int i=0; i < placemarks.getLength(); ++i){
				Element placemark = (Element) placemarks.item(i);
				String name = placemark.getElementsByTagName("name").item(0).getTextContent();
				List<Polygon> poly = new ArrayList<Polygon>();
				NodeList linearRings = placemark.getElementsByTagName("LinearRing");
				for (int j=0; j < linearRings.getLength(); ++j){
					Element coordinates = (Element) ((Element) (linearRings.item(j))).getElementsByTagName("coordinates").item(0);
					String[] coords = coordinates.getTextContent().split(" ");
					List<LatLon> vertices = new ArrayList<LatLon>();
					for (String coord : coords){
						String[] xy = coord.split(",");
						vertices.add(new LatLon(Double.parseDouble(xy[1]), Double.parseDouble(xy[0])));
					}
					poly.add( new Polygon(vertices.toArray(new LatLon[0])) );
				}
				Country c = new Country(name, poly);
				countries.add(c);
			}
		} finally {
			f.close();
		}
		
		System.out.println("Successfully read in polygon information for " + countries.size() + " countries");
		return countries.toArray(new Country[0]);
	}
	
	public static LatLonGrid asLatLonGrid(Country[] countries, double latres, double lonres) {
		int nrows = (int) Math.round(180 / latres);
		int ncols = (int) Math.round(360 / lonres);
		LatLon nwCorner = new LatLon(90,-180);
		LatLonGrid result = new LatLonGrid(nrows, ncols, -1, nwCorner, latres, lonres);
		for (int i=0; i < nrows; ++i){
			for (int j=0; j < ncols; ++j){
				LatLon pt = result.getLocation(i, j);
				result.setValue(i,j, result.getMissing());
				for (int c = 0; c < countries.length; ++c){
					if (countries[c].contains(pt)){
						result.setValue(i, j, c);
						break;
					}
				}
			}
			System.out.println("row " + i + " computed.");
		}
		return result;
	}
	
	public static void main(String[] args) throws Exception {
		CountryPolygons.Country[] countries = CountryPolygons.readKml(CountryPolygons.WORLD_KML);
		for (CountryPolygons.Country c : countries){
			System.out.println(c);
		}
	
		List<LatLon> cities = new ArrayList<LatLon>();
		cities.add(new LatLon(35, -97.1)); // Norman, Oklahoma
		cities.add(new LatLon(40,33)); // Istanbul, Turkey
		cities.add(new LatLon(-34,151)); // Sydney, Australia
		cities.add(new LatLon(-23.5,-46.5)); // Rio, Brazil
		for (LatLon city : cities){
			System.out.println("Looking for " + city);
			for (CountryPolygons.Country c : countries){
				if (c.contains(city)){
					System.out.println(city + " is in " + c);
					break;
				}
			}
		}
		System.out.println("Finished place search using country list");
		
		File out = OutputDirectory.getDefault("countries");
		LatLonGrid grid = asLatLonGrid(countries, 0.1, 0.1);
		for (LatLon city : cities){
			int country = grid.getValue(city);
			if (country >= 0){
				System.out.println("Location " + city + " is in " + countries[country]);
			} else {
				System.out.println("Location " + city + " is unclaimed");
			}
		}		
		
		KmlWriter.write(grid, out, "countries", PngWriter.createRandomColormap());
		EsriGrid.write(grid, out, "countries.txt.gz");
		EsriGrid.write(grid, CountryPolygons.WORLD_GRID);
		
		
/*		// combine with GDI ...
		List<CountryPolygons.Country> notfound = new ArrayList<CountryPolygons.Country>();
		WorldBankGDI.Lookup gdicountries = WorldBankGDI.readAsMap(WorldBankGDI.WORLD);
		for (CountryPolygons.Country c : countries){
			WorldBankGDI.Country match = gdicountries.get(c.name);
			System.out.println(c + " " + match);
			if ( match == null ){
				notfound.add(c);
			}
		}
		for (CountryPolygons.Country c : notfound){
			System.out.println("Not found: " + c);
		}*/
	}
}
