/**
 * 
 */
package edu.ou.asgbook.dataset;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pair;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * To read binary dump output from WDSS-II (http://www.wdssii.org/).
 * 
 * @author v.lakshmanan
 *
 */
public class SeviriInfraredTemperature {
	static {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}
	public static Pair<LatLonGrid,Date> read(File f) throws IOException, ParseException {
		// parse filename: eg: MSG_20050105-150000_556x1111_60.00_-10.00_Channel_09_0.027_0.027.llg
		String[] pieces = f.getName().replace(".llg", "").split("_");
		int pieceno = 0;
		pieceno++; // typeName ignored
		String date = pieces[pieceno++];
		String[] dimpieces = pieces[pieceno++].split("x");
		int numrows = Integer.parseInt(dimpieces[0]);
		int numcols = Integer.parseInt(dimpieces[1]);
		float nwlat = Float.parseFloat(pieces[pieceno++]);
		float nwlon = Float.parseFloat(pieces[pieceno++]);
		++pieceno; // subtype ignored
		int numleft = pieces.length - pieceno - 2;
		pieceno += numleft; // subtype has an underscore
		float deltalat = Float.parseFloat(pieces[pieceno++]);
		float deltalon = Float.parseFloat(pieces[pieceno++]);
		
		// read in LatLonGrid
		FileInputStream fis = new FileInputStream(f);
		byte[] bytes = new byte[numrows*numcols];
		fis.read(bytes);
		LatLonGrid grid = new LatLonGrid(numrows, numcols, 0, new LatLon(nwlat,nwlon), deltalat, deltalon);
		int index = 0;
		for (int i=0; i < numrows; ++i){
			for (int j=0; j < numcols; ++j){
				int value = 256 + bytes[index++]; // 0 to 255
				int reversed = 256 - value; // assign higher value to colder pixels
				grid.setValue(i,j, reversed);
			}
		}
		
		// format date
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmSS");
		Date gridTime = df.parse(date);
		
		System.out.println("Read grid at " + gridTime);
		return new Pair<LatLonGrid,Date>(grid,gridTime);
	}
	
	public static Pair<LatLonGrid,Date>[] readAll(File dir) throws IOException, ParseException {
		File[] files = dir.listFiles(new FileFilter(){
			@Override
			public boolean accept(File f) {
				return f.getName().endsWith(".llg");
			}
		});
		@SuppressWarnings("unchecked")
		Pair<LatLonGrid,Date>[] result = new Pair[files.length];
		for (int i=0; i < result.length; ++i){
			result[i] = read(files[i]);
		}
		return result;
	}
	
	public static void main(String[] args) throws Exception {
		File f = new File("data/seviri");
		Pair<LatLonGrid,Date>[] grids = readAll(f);
		
		// create output directory
		File out = OutputDirectory.getDefault("seviri");
		// write out as image, for viewing
		for (int i=0; i < grids.length; ++i){
			KmlWriter.write(grids[i].first, out, "ir_"+i, PngWriter.createCoolToWarmColormap());
		}
	}
}
