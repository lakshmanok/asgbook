/**
 * 
 */
package edu.ou.asgbook.core;

import java.util.ArrayList;
import java.util.List;



/**
 * A geospatial grid of data in equilat equilon coordinates typically in WGS84 ellipsoid
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class LatLonGrid {
	private int[][] data;
	private int missing;
	private LatLon nwCorner;
	private double latRes;
	private double lonRes;
	
	/**
	 * @param data  Holds on to provided data (does not clone the data)
	 * @param missing  Missing data value, typically -9999 or similar
	 * @param nwCorner the true corner, not the center of the first grid point
	 * @param latres A positive number
	 * @param lonres A positive number
	 */
	public LatLonGrid(int[][] data, int missing, LatLon nwCorner,
			double latres, double lonres) {
		super();
		this.data = data;
		this.missing = missing;
		this.nwCorner = nwCorner;
		this.latRes = latres;
		this.lonRes = lonres;
	}
	
	/**
	 * Make a deep copy
	 */
	public static LatLonGrid copyOf(final LatLonGrid original){
		int[][] copy = new int[original.getNumLat()][original.getNumLon()];
		for (int i=0; i < original.getNumLat(); ++i){
			for (int j=0; j < original.getNumLon(); ++j){
				copy[i][j] = original.data[i][j];
			}
		}
		return new LatLonGrid(copy, original.missing, original.nwCorner, original.latRes, original.lonRes);
	}
	
	/**
	 * Crop this grid.  Does not check dimensions
	 */
	public LatLonGrid crop(int startRow, int startCol, int numLat, int numLon){
		int[][] copy = new int[numLat][numLon];
		for (int i=0; i < numLat; ++i){
			for (int j=0; j < numLon; ++j){
				copy[i][j] = data[i+startRow][j+startCol];
			}
		}
		LatLon origin = this.getLocation(startRow, startCol);
		LatLon nwCorner = new LatLon( origin.getLat() + latRes/2 , origin.getLon() - lonRes/2 );
		return new LatLonGrid(copy, missing, nwCorner, latRes, lonRes);
	}
	
	/**
	 * Initialize a grid of data at zero.
	 * 
	 * @param nrows
	 * @param ncols
	 * @param missing  Missing data value, typically -9999 or similar
	 * @param nwCorner the true corner, not the center of the first grid point
	 * @param latres   A positive number
	 * @param lonres   A positive number
	 */
	public LatLonGrid(int nrows, int ncols, int missing, LatLon nwCorner,
			double latres, double lonres) {
		this( new int[nrows][ncols], missing, nwCorner, latres, lonres );
	}

	public int[][] getData() {
		return data;
	}

	public int getMissing() {
		return missing;
	}

	/**
	 * Note that this is the true corner, not the center of the first grid point.
	 * @return
	 */
	public LatLon getNwCorner() {
		return nwCorner;
	}

	public double getLatRes() {
		return latRes;
	}

	public double getLonRes() {
		return lonRes;
	}
	
	public LatLon getLocation(int row, int col){
		// latitude decreases, longitude increases
		return new LatLon( nwCorner.getLat() - (row+0.5)*latRes,
				nwCorner.getLon() + (col+0.5)*lonRes );
	}
	
	public LatLon getLocation(Pixel p){
		return getLocation(p.getRow(), p.getCol());
	}
	
	public LatLon getLocation(double row, double col){
		// latitude decreases, longitude increases
		return new LatLon( nwCorner.getLat() - (row+0.5)*latRes,
				nwCorner.getLon() + (col+0.5)*lonRes );
	}
	
	/**
	 * This is the true corner, not the middle of the last grid point
	 */
	public LatLon getSeCorner(){
		// latitude decreases, longitude increases
		return new LatLon( nwCorner.getLat() - getNumLat()*latRes,
				nwCorner.getLon() + getNumLon()*lonRes );
	}

	public int getNumLon() {
		return data[0].length;
	}

	public int getNumLat() {
		return data.length;
	}
	
	public int getValue(int row, int col){
		return data[row][col];
	}
	
	public void setValue(int row, int col, int value){
		data[row][col] = value;
	}

	public void setMissing(int i) {
		missing = i;
	}

	/**
	 * The returned row may be outside this grid's dimensions
	 */
	public final int getRow(LatLon location){
		int row = (int) ( (nwCorner.getLat() - location.getLat())/latRes );
		return row;
	}
	
	public final Pixel getPixel(LatLon location){
		int row = getRow(location);
		int col = getCol(location);
		return new Pixel(row, col, data[row][col]);
	}

	/**
	 * The returned col may be outside this grid's dimensions
	 */
	public int getCol(LatLon location) {
		int col = (int) ( (location.getLon() - nwCorner.getLon())/lonRes );
		return col;
	}
	
	public final int getValue(LatLon location) {
		int row = getRow(location);
		int col = getCol(location);
		if ( isValid(row, col) ){
			return data[row][col];
		}
		return missing;
	}

	/**
	 * Are the pixel coordinates in bounds?
	 */
	public final boolean isValid(int row, int col) {
		return row >= 0 && row < data.length && col >= 0 && col < data[row].length;
	}
	
	public void fill(int newval){
		final int nrows = data.length;
		final int ncols = data[0].length;
		for (int i=0; i < nrows; ++i){
			for (int j=0; j < ncols; ++j){
				data[i][j] = newval;
			}
		}
	}

	public void replace(int oldval, int newval){
		final int nrows = data.length;
		final int ncols = data[0].length;
		for (int i=0; i < nrows; ++i){
			for (int j=0; j < ncols; ++j){
				if (data[i][j] == oldval){
					data[i][j] = newval;
				}
			}
		}
	}
	
	public static LatLonGrid add(LatLonGrid a, LatLonGrid b) {
		int nrows = a.getNumLat();
		int ncols = a.getNumLon();
		if (b.getNumLat() != nrows || b.getNumLon() != ncols){
			throw new IllegalArgumentException("Grids are of different dimensions: first grid is " + nrows + "x" + ncols + " while second grid is " + b.getNumLat() + "x" + b.getNumLon());
		}
		LatLonGrid result = LatLonGrid.copyOf(a);
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			if (result.data[i][j] != result.missing){
				int bval = b.data[i][j];
				if (bval != b.missing){
					result.data[i][j] += bval;
				} else {
					result.data[i][j] = result.missing;
				}
			}
		}
		return result;
	}

	public int getValue(Pixel pixel) {
		return getValue(pixel.getX(), pixel.getY());
	}
	
	public Pixel[] asPixels(){
		List<Pixel> pixels = new ArrayList<Pixel>();
		for (int i=0; i < data.length; ++i) for (int j=0; j < data[i].length; ++j){
			if (data[i][j] != missing){
				pixels.add(new Pixel(i,j,data[i][j]));
			}
		}
		return pixels.toArray(new Pixel[0]);
	}
	
	public int[][] longitudewrap(int Ny){
		   int nrows = data.length;
		   int ncols = data[0].length;
		   int hy = Ny/2;
		   int outcols = ncols + 2*hy;
		   int[][] result = new int[nrows][outcols];
		   for (int i=0; i < nrows; ++i) for (int j=0; j < outcols; ++j){
		     int incol = j - hy;
		     if (incol < 0) incol += ncols; // wrap
		     else if (incol >= ncols) incol -= ncols; 
		     result[i][j] = data[i][incol];
		   }
		   return result;
	}

	public LatLonGrid remapTo(LatLonGrid other) {
		LatLonGrid result = LatLonGrid.copyOf(other);
		result.setMissing(this.getMissing());
		for (int i=0; i < other.getNumLat(); ++i){
			int row = getRow( other.getLocation(i,0) );
			for (int j=0; j < other.getNumLon(); ++j){
				int col = getCol( other.getLocation(i,j) );
				if (this.isValid(row,col)){
					result.setValue(i,j, data[row][col]);
				} else {
					result.setValue(i,j, result.missing);
				}
			}
		}
		return result;
	}
}
