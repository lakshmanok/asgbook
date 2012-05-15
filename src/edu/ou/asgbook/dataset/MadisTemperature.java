/**
 * 
 */
package edu.ou.asgbook.dataset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import edu.ou.asgbook.core.PointObservations;

/**
 * Reads the ASCII temperature data available at
 * http://madis-data.noaa.gov/public/sfcdumpguest.html
 * @author Valliappa.Lakshmanan
 *
 */
public class MadisTemperature {

	public static final File TN_Oct2010 = new File("data/madishydro/tn_oct2010_temp.txt");
	
	public static PointObservations read(File file) throws IOException {
		Reader f = null;
		if (file.getAbsolutePath().endsWith(".gz")) {
			f = new InputStreamReader(new GZIPInputStream(new FileInputStream(
					file)));
		} else {
			f = new FileReader(file);
		}
		return read(f);
	}
	
	@SuppressWarnings("unused")
	public static PointObservations read(Reader r) throws IOException {
		Scanner s = new Scanner(r);
		List<PointObservations.ObservationPoint> result = new ArrayList<PointObservations.ObservationPoint>();
		
		final int FACTOR = 10;
		final int MISSING = -99999 * FACTOR;
		s.nextLine(); // header
		while (s.hasNext()){
			String station = s.next();
			String date = s.next();
			String time = s.next();
			int precip = (int) Math.round( s.nextDouble() * FACTOR );
			double lat = s.nextDouble();
			double lon = s.nextDouble();
			result.add(new PointObservations.ObservationPoint(lat,lon,precip));
		}
		
		PointObservations.ObservationPoint[] pts = result.toArray(new PointObservations.ObservationPoint[0]);
		return new PointObservations(pts, MISSING);
	}
	
	public static void main(String[] args) throws Exception {
		PointObservations data = MadisTemperature.read(MadisTemperature.TN_Oct2010);
		for (int i=0; i < data.getPoints().length; ++i){
			System.out.println(data.getPoints()[i]);
		}
	}
}
