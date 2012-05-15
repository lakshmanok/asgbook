/**
 * 
 */
package edu.ou.asgbook.oban;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.PointObservations;

/**
 * Utility functions for objective analysis
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class ObjectiveAnalysisUtils {
	public static LatLonGrid createBoundingGrid(PointObservations data, double latres, double lonres){		
		PointObservations.ObservationPoint[] points = data.getPoints();
		if ( points.length == 0 ){
			throw new IllegalArgumentException("Number of points has be greater than zero");
		}
		
		// find bounding box
		double minlat = 90;
		double maxlat = -90;
		double minlon = 180;
		double maxlon = -180;
		for (int i=0; i < points.length; ++i){
			if (points[i].getLat() > maxlat){
				maxlat = points[i].getLat();
			}
			if (points[i].getLat() < minlat){
				minlat = points[i].getLat(); 
			}
			if (points[i].getLon() > maxlon){
				maxlon = points[i].getLon();
			}
			if (points[i].getLon() < minlon){
				minlon = points[i].getLon();
			}
		}
		
		// go a little bit off to the side and roundoff so that grid bounds are multiples of res
		minlat = round(minlat - latres, latres);
		maxlat = round(maxlat + latres, latres);
		minlon = round(minlon - lonres, lonres);
		maxlon = round(maxlon + lonres, lonres);
		
		int nrows = (int) Math.round((maxlat - minlat)/latres);
		int ncols = (int) Math.round((maxlon - minlon)/lonres);
		
		System.out.println(points.length + " points will fit inside a " + nrows + "x" + ncols + " grid");
		return new LatLonGrid(nrows, ncols, data.getMissing(), new LatLon(maxlat,minlon), latres, lonres);
	}
	
	private static double round(double value, double delta){
		return Math.round(value / delta) * delta;
	}

	public static double computeMeanDistance(PointObservations data) {
		PointObservations.ObservationPoint[] points = data.getPoints();
		if ( points.length < 1 ){
			throw new IllegalArgumentException("Number of points has be greater than one");
		}
		
		double totdist = 0;
		for (int i=0; i < points.length; ++i){
			double mindistsq = Double.MAX_VALUE;
			for (int j=0; j < points.length; ++j){
				if ( j != i ){
					double latdist = points[i].getLat() - points[j].getLat();
					double londist = points[i].getLon() - points[j].getLon();
					double distsq = (latdist*latdist + londist*londist);
					if ( distsq < mindistsq ){
						mindistsq = distsq;
					}
				}
			}
			totdist += Math.sqrt(mindistsq);
		}
		
		return totdist / points.length;
	}
}
