/**
 * 
 */
package edu.ou.asgbook.filters;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;

/**
 * Finds the highest value pixel in the image
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class MaxValueFilter {

	public static class Result {
		public final int value;
		public LatLon location;
		public Result(int value, LatLon location) {
			this.value = value;
			this.location = location;
		}
	}
	
	public Result findHighestValued(LatLonGrid input){
		int[][] data = input.getData();
		int x = -1;
		int y = -1;
		int maxval = input.getMissing();
		for (int i=0; i < input.getNumLat(); ++i){
			for (int j=0; j < input.getNumLon(); ++j){
				if ( data[i][j] != input.getMissing() ){
					if ( maxval == input.getMissing() ||
						 maxval < data[i][j] ){
						x = i; // new maximum
						y = j;
						maxval = data[x][y];
					}
				}
			}
		}
		if ( x >=0 && y >= 0 ){
			LatLon loc = input.getLocation(x, y);
			return new Result(data[x][y], loc);
		}
		return null;
	}
	
	public static void main(String[] args) throws Exception {
		// read input
		LatLonGrid popdensity = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA);
		popdensity.setMissing(0); // will get to process less data this way
		
		// find 10 highest
		MaxValueFilter filter = new MaxValueFilter();
		Result result = filter.findHighestValued(popdensity);
		System.out.println("Maximum is " + result.value + " at " + result.location);
	}
}
