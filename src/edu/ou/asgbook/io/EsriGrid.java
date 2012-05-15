/**
 * 
 */
package edu.ou.asgbook.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.linearity.DataTransform;

/**
 * Read an ESRI grid.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class EsriGrid {
	public static LatLonGrid read(File file, DataTransform t) throws IOException, FileNotFoundException {
		Reader f = null;
		if (file.getAbsolutePath().endsWith(".gz")) {
			f = new InputStreamReader(new GZIPInputStream(new FileInputStream(
					file)));
		} else {
			f = new FileReader(file);
		}
		return read(f, t);
	}

	public static LatLonGrid read(Reader inputFile, DataTransform t) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(inputFile);
			// fields separated by spaces. This is the regular expression for spaces
			final String sep = " +";
			// read header
			int ncols = Integer.parseInt( reader.readLine().split(sep)[1] );
			int nrows = Integer.parseInt( reader.readLine().split(sep)[1] );
			double cornerlon = Double.parseDouble( reader.readLine().split(sep)[1] );
			double cornerlat = Double.parseDouble( reader.readLine().split(sep)[1] );
			double latres = Double.parseDouble( reader.readLine().split(sep)[1] );
			double lonres = latres;
			String missingValue = reader.readLine().split(sep)[1];
			int missing = Integer.parseInt(missingValue);

			// read in data
			int[][] data = new int[nrows][ncols];
			int numvalid = 0;
			int nummissing = 0;
			int numzero = 0;
			int minval = Integer.MAX_VALUE;
			int maxval = 0;
			int i = 0;
			int j = 0;
			String line = null;
			while ( (line = reader.readLine()) != null ){
				for (String field : line.split(sep)){
					if (field.equals(missingValue)){
						data[i][j] = missing;
						++nummissing;
					} else {
						double value = Double.parseDouble(field);
						data[i][j] = t.transformAndRoundoff(value);
						if ( data[i][j] != 0 ){
							++numvalid;
							minval = Math.min(minval, data[i][j]);
							maxval = Math.max(maxval, data[i][j]);
						} else {
							++numzero;
						}
					}
					++j; // next column
					if ( j == ncols ){
						j = 0; // next row
						++i;
					}
				}
			}
			System.out.println(numvalid + " valid pixels; " + numzero + " zero; " + nummissing + " " + missing + " range=[" + minval + "," + maxval +"]");
			LatLon nwCorner = new LatLon(cornerlat + latres*nrows, cornerlon);
			return new LatLonGrid(data, missing, nwCorner, latres, lonres);
		} catch (Exception e){
			System.err.println("Error reading file: " + e);
			throw new IllegalArgumentException(e);
		} finally {
			if (reader != null) {
				try{
					reader.close();
				} catch (Exception e){
					// okay
				}
			}
		}
	}

	public static void write(LatLonGrid data, File outdir, String fname) throws IOException {
		File f = new File(outdir.getAbsolutePath() + "/" + fname);
		write(data, f);
	}
	
	public static void write(LatLonGrid data, File out) throws IOException {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new GZIPOutputStream(new FileOutputStream(out)));
			int nrows = data.getNumLat();
			int ncols = data.getNumLon();
			writer.println("ncols " + ncols);
			writer.println("nrows " + nrows);
			writer.println("xllcorner " + data.getNwCorner().getLon());
			writer.println("yllcorner " + (data.getNwCorner().getLat() - data.getLatRes()*data.getNumLat()));
			writer.println("cellsize " + data.getLatRes());
			writer.println("NODATA_value " + data.getMissing());

			final String sep = " ";
			for (int i=0; i < nrows; ++i){
				for (int j=0; j < ncols; ++j){
					writer.print(data.getValue(i,j));
					if (j != (ncols-1)){
						writer.print(sep);
					}
				}
				writer.println();
			}
		} finally {
			if (writer != null){
				System.out.println("Successfully wrote " + out);
				writer.close();
			}
		}
	}
	
}
