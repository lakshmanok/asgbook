package edu.ou.asgbook.rasterization;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.ScalarStatistic;

/**
 * A rectangular bounding box of a polygon.
 * 
 * It can sometimes be cheaper to use a bounding box instead of the
 * accurate locations and do the real calcuation only if the bounding
 * box passes.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class BoundingBox {
	private double minx;
	private double miny;
	private double maxx;
	private double maxy;

	public BoundingBox(LatLon[] vertices) {
		ScalarStatistic lat = new ScalarStatistic();
		ScalarStatistic lon = new ScalarStatistic();
		for (int i=0; i < vertices.length; ++i){
			lat.update(vertices[i].getLat());
			lon.update(vertices[i].getLon());
		}
		maxx = lat.getMax();
		maxy = lon.getMax();
		minx = lat.getMin();
		miny = lon.getMin();
	}

	public boolean contains(double x, double y){
		return (x >= minx && x <= maxx && y >= miny && y <= maxy); 
	}
	
	private BoundingBox(){
		// for clone operation only
	}
	
	public static BoundingBox copyOf(BoundingBox a){
		BoundingBox b = new BoundingBox();
		b.minx = a.minx;
		b.maxx = a.maxx;
		b.miny = a.miny;
		b.maxy = a.maxy;
		return b;
	}
	
	public void update(BoundingBox a){
		minx = Math.min(minx, a.minx);
		miny = Math.min(miny, a.miny);
		maxx = Math.max(maxx, a.maxx);
		maxy = Math.max(maxy, a.maxy);
	}
}