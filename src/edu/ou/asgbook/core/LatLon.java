/**
 * 
 */
package edu.ou.asgbook.core;

/**
 * 
 * A point on the earth's surface typically in WGS84
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class LatLon {
	private double lat;
	private double lon;
	public double getLat() {
		return lat;
	}
	public double getLon() {
		return lon;
	}
	public LatLon(double lat, double lon) {
		super();
		this.lat = lat;
		this.lon = lon;
	}
	
	private final static double sq(double x){
		return x*x;
	}

	public double distanceInKms(LatLon other){
		double lat1 = Math.toRadians(this.lat);
		double lat2 = Math.toRadians(other.lat);
		double lon1 = Math.toRadians(this.lon);
		double lon2 = Math.toRadians(other.lon);
		
		// double R = 6371; // spherical earth radius
		double lat0 = (lat2+lat1)/2;
		double a = 6378.137; // WGS-84
		double f = 1.0/298.257223563;
		double esq = f*(2-f);
		double R=a * (1-esq)/Math.pow(sq(1-esq*(Math.sin(lat0))),1.5);
		
		double dlon = lon2 - lon1;
		double dlat = lat2 - lat1;
		double term = sq(Math.sin(dlat/2)) + Math.cos(lat1) * Math.cos(lat2) * sq(Math.sin(dlon/2));
		return (2 * R * Math.asin(Math.min(1,Math.sqrt(term))));
	}
	
	@Override
	public String toString() {
		return new StringBuilder().append("[").append(lat).append(",")
				.append(lon).append("]").toString();
	}
	
	public static void main(String[] args){
		LatLon pt1 = new LatLon(35,-97);
		LatLon pt2 = new LatLon(35.01, -97);
		LatLon pt3 = new LatLon(35, -97.01);
		System.out.println("sph: 0.01 in lat = " + pt1.distanceInKms(pt2) + " kms at " + pt1);
		System.out.println("sph: 0.01 in lon = " + pt1.distanceInKms(pt3) + " kms at " + pt1);
	}
}
