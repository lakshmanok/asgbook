/**
 * 
 */
package edu.ou.asgbook.rasterization;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * 
 * A line that connects two points on the earth's surface.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class Line {
	private double lat0, lon0, lat1, lon1;

	public Line(double lat0, double lon0, double lat1, double lon1) {
		this.lat0 = lat0;
		this.lon0 = lon0;
		this.lat1 = lat1;
		this.lon1 = lon1;
	}
	
	public Line(LatLon p0, LatLon p1) {
		this.lat0 = p0.getLat();
		this.lon0 = p0.getLon();
		this.lat1 = p1.getLat();
		this.lon1 = p1.getLon();
	}
	
	public double getLat0() {
		return lat0;
	}

	public double getLon0() {
		return lon0;
	}

	public double getLat1() {
		return lat1;
	}

	public double getLon1() {
		return lon1;
	}

	public List<Pixel> getPositionIn(LatLonGrid grid){
		List<Pixel> result = new ArrayList<Pixel>();
		Pixel p0 = grid.getPixel( new LatLon(lat0, lon0) );
		Pixel p1 = grid.getPixel( new LatLon(lat1, lon1) );
		System.out.println("Line from " + p0 + " to " + p1);
		int rowlen = Math.abs(p0.getRow() - p1.getRow());
		int collen = Math.abs(p0.getCol() - p1.getCol());
		// avoid divide by zero in slope calculations below
		if ( rowlen == 0 && collen == 0){
			result.add(p0);
			return result;
		}
		if ( rowlen > collen ){
			// increment in row
			int startrow = Math.min(p0.getRow(), p1.getRow());
			int endrow = Math.max(p0.getRow(), p1.getRow());
			double slope = (p1.getCol() - p0.getCol())/((double)(p1.getRow()-p0.getRow()));
			for (int row=startrow; row <= endrow; ++row){
				int col = (int) Math.round(slope*(row-p0.getRow())+p0.getCol());
				if (grid.isValid(row, col)){
					result.add( new Pixel(row, col, grid.getValue(row, col)) );
				}
			}
		} else {
			int startcol = Math.min(p0.getCol(), p1.getCol());
			int endcol = Math.max(p0.getCol(), p1.getCol());
			double slope = (p1.getRow()-p0.getRow())/((double)(p1.getCol()-p0.getCol()));
			for (int col=startcol; col <= endcol; ++col){
				int row = (int) Math.round(slope*(col-p0.getCol())+p0.getRow());
				if (grid.isValid(row, col)){
					result.add( new Pixel(row, col, grid.getValue(row, col)) );
				}
			}
		}
		return result;
	}
	
	/** Is x in between x0 and x1? */
	private static boolean isBetween(double x0, double x, double x1) {
		return ((x - x0) * (x1 - x) > 0 || (x1 == x));
	}
	
	/** Find the intersection point. Returns null if not in range. */
	public Double getXIntercept(double y) {
		if (!isBetween(lon0, y, lon1)) {
			return null;
		}
		// if y0=y1, then inrange would be false
		double x;
		if (lon0 != lon1) {
			x = lat0 + (y - lon0) * (lat1 - lat0) / (lon1 - lon0);
		} else {
			x = (lat1 + lat0) / 2;
		}
		return x;
	}

	/** Find the intersection point. Returns null if not in range. */
	public Double getYIntercept(double x) {
		if (!isBetween(lat0, x, lat1)) {
			return null;
		}
		double y;
		if (lat0 != lat1) {
			y = lon0 + (x - lat0) * (lon1 - lon0) / (lat1 - lat0);
		} else {
			y = (lon1 + lon0) / 2;
		}
		return y;
	}
	
	public static void main(String args[]) throws Exception {
		LatLonGrid grid = new LatLonGrid(100,100,0,new LatLon(100,-90),0.01,0.01);
		List<Pixel> ver = new Line(99.3,-89.3,99.7,-89.4).getPositionIn(grid);
		List<Pixel> hor = new Line(99.3,-89.3,99.4,-89.7).getPositionIn(grid);
		for (Pixel p : ver){
			grid.setValue(p.getRow(), p.getCol(), 10);
		}
		for (Pixel p : hor){
			grid.setValue(p.getRow(), p.getCol(), 20);
		}
		
		File out = OutputDirectory.getDefault("raster");
		KmlWriter.write(grid, out, "drawlines", PngWriter.createCoolToWarmColormap());
	}
}
