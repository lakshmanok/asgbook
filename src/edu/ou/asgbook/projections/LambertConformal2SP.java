package edu.ou.asgbook.projections;

import edu.ou.asgbook.core.LatLon;

/**
 * 
 * Lambert Conformation 2 Standard Parallels map projection.
 * 
 * @author v.lakshmanan
 *
 */
public class LambertConformal2SP {
	
	public static class Coord {
		public final double northing, easting;
		public Coord(double northing, double easting) {
			super();
			this.northing = northing;
			this.easting = easting;
		}
		@Override
		public String toString() {
			return new StringBuilder().append("[").append(northing).append("N,")
					.append(easting).append("E]").toString();
		}
	}
	
	private final Ellipsoid ellipsoid;
	private final LatLon false_origin_ll;
	private final double lat_1, lat_2;
	private final Coord false_origin_lam;
	private final double e, n, F, rF;
	
	public LambertConformal2SP(Ellipsoid ellipsoid, LatLon falseOriginLl,
			double lat_1, double lat_2, Coord falseOriginLam) {
		this.ellipsoid = ellipsoid;
		this.false_origin_ll = falseOriginLl;
		this.lat_1 = lat_1;
		this.lat_2 = lat_2;
		this.false_origin_lam = falseOriginLam;
		
		this.e = Math.sqrt(ellipsoid.eccsq);
		double phi1 = Math.toRadians(this.lat_1);
		double phi2 = Math.toRadians(this.lat_2);
		double t1 = compute_t(e,phi1);
		double t2 = compute_t(e,phi2);
		double m1 = compute_m(e,phi1);
		double m2 = compute_m(e,phi2);
		
		this.n = (Math.log(m1) - Math.log(m2))/(Math.log(t1) - Math.log(t2));
		this.F = m1 / (n*Math.pow(t1,n));
		
		double phiF = Math.toRadians(false_origin_ll.getLat());
		double tF = compute_t(e, phiF);
		this.rF = ellipsoid.eqr * F * Math.pow(tF, n);
	}

	public Coord getLambert(LatLon in){
		double phi = Math.toRadians(in.getLat());
		double t = compute_t(e,phi);
		double r = ellipsoid.eqr * F * Math.pow(t,n);
		double lambda = Math.toRadians(in.getLon());
		double lambdaF = Math.toRadians(false_origin_ll.getLon());
		double theta = n * (lambda - lambdaF);
		
		double easting = false_origin_lam.easting + r * Math.sin(theta);
		double northing = false_origin_lam.northing + rF - r * Math.cos(theta);
		return new Coord(northing, easting);
	}
	
	public LatLon getLatLon (Coord lam){
	  double eastdiff = (lam.easting - false_origin_lam.easting);
	  double northdiff = (lam.northing - false_origin_lam.northing);
	  double rFnorthdiff = rF - northdiff;
	  double r = Math.sqrt( eastdiff*eastdiff + rFnorthdiff*rFnorthdiff );
	  if ( n < 0 ) r = -r;
	  double t = Math.pow( r / (ellipsoid.eqr*F) , 1/n );
	  double theta = Math.atan( eastdiff/rFnorthdiff );
	  
	  double lon = Math.toDegrees(theta/n) + false_origin_ll.getLon();
	  
	  // iterate to find phi
	  double phi = Math.PI/2 - 2 * Math.atan(t);
	  double old_phi;
	  int iter=0;
	  do{
	    old_phi = phi;
	    ++iter;
	    phi = Math.PI/2 - 2 * Math.atan( t*Math.pow( (1-e*Math.sin(phi))/(1+e*Math.sin(phi)), e/2 ) );
	  } while ( Math.abs(phi-old_phi) > 0.00001 && iter < 5 );

	  double lat = Math.toDegrees(phi);
	  return new LatLon( lat, lon );
	}

	
	private static double compute_t(double e, double phi) {
		double esinphi = e*Math.sin(phi);
		return Math.tan(Math.PI/4 - phi/2)/Math.pow( (1-esinphi)/(1+esinphi), e/2 );
	}
	
	private static double compute_m(double e, double phi){
		double esinphi = e*Math.sin(phi);
		return Math.cos(phi)/Math.sqrt(1-esinphi*esinphi);
	}
	
	public static void main(String[] args){
		LambertConformal2SP conv = new LambertConformal2SP(Ellipsoid.WGS84(), new LatLon(51,-127), 43.5, 28.5, new Coord(0,0));
		LatLon ll = new LatLon(36,-96);
		LambertConformal2SP.Coord lam = conv.getLambert(ll);
		LatLon ll2 = conv.getLatLon(lam);
		System.out.println(ll + "->" + lam + "->" + ll2);
		
		ll = new LatLon(51,-96);
		lam = conv.getLambert(ll);
		ll2 = conv.getLatLon(lam);
		System.out.println(ll + "->" + lam + "->" + ll2);
	
		ll = new LatLon(21,-96);
		lam = conv.getLambert(ll);
		ll2 = conv.getLatLon(lam);
		System.out.println(ll + "->" + lam + "->" + ll2);
		
		ll = new LatLon(36,-127);
		lam = conv.getLambert(ll);
		ll2 = conv.getLatLon(lam);
		System.out.println(ll + "->" + lam + "->" + ll2);
	
		ll = new LatLon(36,-65);
		lam = conv.getLambert(ll);
		ll2 = conv.getLatLon(lam);
		System.out.println(ll + "->" + lam + "->" + ll2);
	}
}
