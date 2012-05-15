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
 * A polygon consisting of straight edges along the earth's surface.
 * 
 * @author valliappa.lakshmanan
 * 
 */
public class Polygon {
	private List<Line> edges = new ArrayList<Line>();
	private double area;
	private LatLon centroid;
	private BoundingBox boundingBox;

	public BoundingBox getBoundingBox() {
		return boundingBox;
	}

	public Polygon(LatLon[] vertices){
		if (vertices.length < 3){
			throw new IllegalArgumentException("Need atleast 3 vertices for polygon.");
		}
		for (int i=0; i < vertices.length-1; ++i){
			Line line = new Line( vertices[i].getLat(), vertices[i].getLon(),
					vertices[i+1].getLat(), vertices[i+1].getLon() );
			edges.add(line);
		}
		
		// connect start and end point
		LatLon last = vertices[vertices.length-1];
		LatLon first = vertices[0];
		edges.add(new Line(last.getLat(),last.getLon(),first.getLat(),first.getLon()));
		computeAreaAndCentroid();
		boundingBox = new BoundingBox(vertices);
	}

	public List<Line> getEdges() {
		return edges;
	}

	public LatLon getCentroid() {
		return centroid;
	}

	/**
	 * The area is in degrees^2, so not very useful unless you can
	 * convert to km^2.
	 */
	public double getArea() {
		return area;
	}

	private void computeAreaAndCentroid() {
		// area
		area = 0;
		int N = edges.size();
		for (int i = 0; i < N; ++i) {
			double a = edges.get(i).getLat0() * edges.get(i).getLon1(); // x_i * y_{i+1}
			double b = edges.get(i).getLat1() * edges.get(i).getLon0(); // x_{i+1} * y_i
			area = area + a - b;
		}
		area = (area / 2);

		// centroid
		double denom = 6 * area;
		double clat = 0;
		double clon = 0;
		for (int i = 0; i < N; ++i) {
			double wt = (edges.get(i).getLat0() * edges.get(i).getLon1() - 
					edges.get(i).getLat1()* edges.get(i).getLon0());
			clat = clat + (edges.get(i).getLat0() + edges.get(i).getLat1()) * wt;
			clon = clon + (edges.get(i).getLon0() + edges.get(i).getLon1()) * wt;
		}
		clat = clat / denom;
		clon = clon / denom;

		// don't care about whether points are clockwise or counterclockwise
		area = Math.abs(area);
		centroid = new LatLon(clat, clon);
	}

	/**
	 * Workhorse method: finds out if this point is within this polygon.
	 */
	public boolean contains(double x, double y) {
		// as an optimization, check the bounding box first
		if (!boundingBox.contains(x,y)){
			return false;
		}
		
		int num_xcrossing = 0;
		int num_ycrossing = 0;
		for (int i = 0; i < edges.size(); ++i) {
			Double x_intercept = edges.get(i).getXIntercept(y);
			Double y_intercept = edges.get(i).getYIntercept(x);
			if (y_intercept != null) {
				if (y_intercept >= y) {
					++num_ycrossing;
				}
			}
			if (x_intercept != null) {
				if (x_intercept >= x) {
					++num_xcrossing;
				}
			}
		}
		// odd number of crossings means inside
		return ((num_xcrossing % 2 == 1) && (num_ycrossing % 2 == 1));
	}

	
	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("rasterpolygon");
		
		// made up
		LatLonGrid grid = new LatLonGrid(500, 500, 0, new LatLon(20, -10), 0.01, 0.01);
		LatLon[] vertices = new LatLon[]{
				new LatLon(19,-7),
				new LatLon(17.5,-6),
				new LatLon(16.5,-6.8),
				new LatLon(17.2,-8.5),
				new LatLon(16,-9.5),
				new LatLon(17,-9)
		};
		Polygon poly = new Polygon(vertices);

		// draw edges
		final int EDGE = 10;
		for (Line line : poly.getEdges()){
			for (Pixel p : line.getPositionIn(grid)){
				grid.setValue(p.getRow(), p.getCol(), EDGE);
			}
		}
		KmlWriter.write(grid, out, "edges", PngWriter.createCoolToWarmColormap());

		
		// fill points inside
		final int POLY = 5;
		int npix = 0;
		for (int i=0; i < grid.getNumLat(); ++i){
			for (int j=0; j < grid.getNumLon(); ++j){
				LatLon loc = grid.getLocation(i, j);
				if ( poly.contains(loc.getLat(), loc.getLon())){
					grid.setValue(i,j, POLY);
					++npix;
				}
			}
		}
		KmlWriter.write(grid, out, "polygon", PngWriter.createCoolToWarmColormap());
	
		System.out.println("Area of polygon: " + poly.getArea() + " num-pixels colored=" + npix);
		System.out.println("Centroid of polygon: " + poly.getCentroid());
	}
	
}
