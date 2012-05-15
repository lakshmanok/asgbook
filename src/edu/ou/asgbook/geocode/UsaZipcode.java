/**
 * 
 */
package edu.ou.asgbook.geocode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import edu.ou.asgbook.core.LatLon;

/**
 * 
 * Find the city for each zipcode in the USA.
 * 
 * @author v.lakshmanan
 *
 */
public class UsaZipcode {

	public static class Entry {
		public final LatLon location;
		public final String zipcode;
		public final String city;
		public final String state;
		public final String county;
		public Entry(LatLon location, String zipcode, String city, String state, String county) {
			this.location = location;
			this.zipcode = zipcode;
			this.city = city;
			this.state = state;
			this.county = county;
		}
		@Override
		public String toString(){
			return city + "," + state + " zip=" + zipcode + " loc=" + location;
		}
	}
	
	private final Entry[] entries;
	private static final UsaZipcode instance = new UsaZipcode();
	
	private UsaZipcode() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("edu/ou/imganalysis/geocode/us_zipcodes.csv")));
		List<Entry> e = new ArrayList<UsaZipcode.Entry>();
		String line = null;
		try {
			while ( (line = reader.readLine()) != null){
				String[] pieces = line.split(",");
				if (pieces.length > 5 && pieces[1].length() > 0){
					String zipcode = pieces[0];
					double lat = Double.parseDouble(pieces[1]);
					double lon = Double.parseDouble(pieces[2]);
					String city = pieces[3];
					String state = pieces[4];
					String county = pieces[5];
					e.add(new Entry(new LatLon(lat,lon),zipcode,city,state,county));
				}
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		this.entries = e.toArray(new Entry[0]);
	}
	
	public Entry getEntryClosestTo(LatLon loc){
		double mindistsq = 0.5*0.5; // within 50 km
		Entry best = null;
		for (Entry e : entries){
			double dist_lat = e.location.getLat() - loc.getLat();
			double dist_lon = e.location.getLon() - loc.getLon();
			double dist_sq = dist_lat*dist_lat + dist_lon*dist_lon;
			if ( dist_sq < mindistsq ){
				mindistsq = dist_sq;
				best = e;
			}
		}
		return best;
	}
	
	public static UsaZipcode getInstance(){
		return instance;
	}
	
	public static void main(String[] args) {
		Entry e = UsaZipcode.getInstance().getEntryClosestTo(new LatLon(18.31,-66.06));
		System.out.println(e);
		
		e = UsaZipcode.getInstance().getEntryClosestTo(new LatLon(35.2,-97.4));
		System.out.println(e);
	}

}
