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
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.projections.Ellipsoid;
import edu.ou.asgbook.projections.LambertConformal2SP;
import edu.ou.asgbook.projections.Remapper;

/**
 * 
 * Reads lambert-conformal ascii grid
 * 
 * @author v.lakshmanan
 *
 */
public class SurfaceAlbedo {
	public static File CONUS = new File("data/sfcalbedo/sfcalbedo.txt.gz");
	
	public static LatLonGrid read(Reader inputFile, int scaling) {
		Scanner s = null;
		try {
			s = new Scanner(inputFile);
			
			// read header
			@SuppressWarnings("unused")
			String junk;
			junk = s.next(); String ellipsoid = s.next();
			junk = s.next(); String projection = s.next();
			if (! (ellipsoid.equals("WGS-84") && projection.equals("LAMBERT2SP"))){
				throw new IllegalArgumentException("Expect data to be in LAMBERT2SP and WGS-84 not " + projection + " and " + ellipsoid);
			}
			junk = s.next(); double lat1 = s.nextDouble();
			junk = s.next(); double lat2 = s.nextDouble();
			junk = s.next(); double center_lat = s.nextDouble();
			junk = s.next(); double center_lon = s.nextDouble();
			junk = s.next(); double eastres = s.nextDouble(); // meters
			junk = s.next(); double northres = s.nextDouble();
			junk = s.next(); int nrows = s.nextInt();
			junk = s.next(); int ncols = s.nextInt();
			
			double center_northing = - nrows * 0.5 * northres;
			double center_easting = ncols * 0.5 * eastres;
			
			int missing = -999; // doesn't exist in the data
			
			// read in data (in Lambert projection)
			int[][] lamdata = new int[nrows][ncols];
			for (int i=0; i < nrows; ++i){
				for (int j=0; j < ncols; ++j){
					try {
						double value = s.nextDouble();
						lamdata[i][j] = (int)(0.5 + value * scaling);
					} catch (Exception e){
						lamdata[i][j] = missing;
					}
				}
			}
			
			// Find grid extent
			LambertConformal2SP proj = new LambertConformal2SP(Ellipsoid.WGS84(), new LatLon(center_lat,center_lon), lat1, lat2, new LambertConformal2SP.Coord(center_northing, center_easting));
			double minlat = 180; double maxlat = -180;
			double minlon = 180; double maxlon = -180;
			for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
				LambertConformal2SP.Coord lam = new LambertConformal2SP.Coord(-(i+0.5)*northres, (j+0.5)*eastres);
				LatLon ll = proj.getLatLon(lam);
				if ( ll.getLat() < minlat ) minlat = ll.getLat();
				if ( ll.getLat() > maxlat ) maxlat = ll.getLat();
				if ( ll.getLon() < minlon ) minlon = ll.getLon();
				if ( ll.getLon() > maxlon ) maxlon = ll.getLon();
			}
			System.out.println("Grid extent: " + minlat + " " + minlon + " " + maxlat + " " + maxlon);
			
			// best latres, lonres
			int outrows = nrows;
			int outcols = ncols;
			double latres = (maxlat - minlat)/outrows;
			double lonres = (maxlon - minlon)/outcols;
			// lookup nearest neighbor in lat-lon space
			int[][] lldata = new int[outrows][outcols];
			for (int i=0; i < outrows; ++i){
				double lat = maxlat - i * latres;
				for (int j=0; j < outcols; ++j){
					double lon = minlon + j * lonres;
					LambertConformal2SP.Coord lam = proj.getLambert( new LatLon(lat,lon) );
					double rowno = (0 - lam.northing)/northres; 
					rowno = nrows - rowno - 1; // row=0 is southmost row
					double colno = (lam.easting - 0)/eastres;
					// lldata[i][j] = Remapper.nearestNeighbor(rowno, colno, lamdata, missing);
					lldata[i][j] = Remapper.bilinearInterpolation(rowno, colno, lamdata, missing);
				}
			}
			
			return new LatLonGrid(lldata, missing, new LatLon(maxlat,minlon), latres, lonres);
		} catch (Exception e){
			System.err.println("Error reading file: " + e);
			throw new IllegalArgumentException(e);
		} finally {
			if (s != null) {
				try{
					s.close();
				} catch (Exception e){
					// okay
				}
			}
		}
	}
	
	/**
	 * reads data from a File. The File can be gzipped or uncompressed.
	 */
	public static LatLonGrid read(File file, int scaling) throws IOException {
		Reader f = null;
		System.out.println("Reading " + file.getAbsolutePath());
		if (file.getAbsolutePath().endsWith(".gz")) {
			f = new InputStreamReader(new GZIPInputStream(new FileInputStream(
					file)));
		} else {
			f = new FileReader(file);
		}
		return read(f, scaling);
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("sfcalbedo");
		
		// read input
		LatLonGrid conus = SurfaceAlbedo.read(SurfaceAlbedo.CONUS, 100);
		
		// write out as image, for viewing
		KmlWriter.write(conus, out, "sfcalbedo", PngWriter.createCoolToWarmColormap());
	}
}
